package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.infrastructure.loader.MarketPriceService;
import com.kacemrisk.market.infrastructure.loader.PortfolioLoader;
import com.kacemrisk.market.infrastructure.model.RiskPosition;
import com.kacemrisk.market.workflow.RunContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapGroupsFunction;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class JoinAdapter {

    private final SparkSession spark;
    private final PortfolioLoader portfolioLoader;
    private final MarketPriceService marketPriceService;

    public Dataset<RiskPosition> enrich(RunContext ctx) {

        Dataset<Row> portfolioDs = portfolioLoader.load().cache();

        if (portfolioDs.isEmpty()) {
            log.error("[JoinAdapter] Portfolio dataset is empty — aborting.");
            portfolioDs.unpersist();
            return spark.emptyDataset(Encoders.kryo(RiskPosition.class));
        }

        // ── Phase A — collect unique ticker→assetClass (reuses cached dataset) ──
        Map<String, AssetClass> tickerAssetClass = portfolioDs
                .select("ticker", "assetClass")
                .distinct()
                .collectAsList()
                .stream()
                .flatMap(r -> {
                    AssetClass ac = AssetClass.of(r.getAs("assetClass"));
                    if (ac == null) {
                        log.warn("[JoinAdapter] Unknown asset class '{}' for ticker '{}' — skipping",
                                r.getAs("assetClass"), r.getAs("ticker"));
                        return Stream.empty();
                    }
                    return Stream.of(Map.entry((String) r.getAs("ticker"), ac));
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        log.info("[JoinAdapter] Phase A — {} unique ticker(s)", tickerAssetClass.size());

        // ── Phase B — single loadPrices call, then broadcast ─────────────────────
        var from = ctx.asOfDate().minusDays(ctx.historicalWindow());
        Map<String, double[]> pricesByTicker = marketPriceService.loadPrices(
                tickerAssetClass, from, ctx.asOfDate());

        if (pricesByTicker.isEmpty()) {
            log.error("[JoinAdapter] No prices loaded for window [{} → {}] — aborting.", from, ctx.asOfDate());
            portfolioDs.unpersist();
            return spark.emptyDataset(Encoders.kryo(RiskPosition.class));
        }

        Broadcast<Map<String, double[]>> priceBroadcast =
                JavaSparkContext.fromSparkContext(spark.sparkContext()).broadcast(pricesByTicker);

        // ── Phase C — enrich via broadcast, return lazy Dataset ──────────────────
        // Caller is responsible for collectAsList(), then priceBroadcast.unpersist()
        // and portfolioDs.unpersist() to release memory.
        return portfolioDs
                .groupByKey(
                        (MapFunction<Row, String>) row -> row.getAs("ticker"),
                        Encoders.STRING())
                .flatMapGroups(
                        (FlatMapGroupsFunction<String, Row, RiskPosition>) (ticker, rowIter) -> {
                            double[] history = priceBroadcast.value().getOrDefault(ticker, new double[0]);
                            double spot = history.length > 0 ? history[history.length - 1] : 0.0;
                            List<RiskPosition> out = new ArrayList<>();
                            rowIter.forEachRemaining(row -> out.add(RiskPosition.builder()
                                    .portfolioId(row.getAs("portfolioId"))
                                    .ticker(ticker)
                                    .quantity(row.getAs("quantity"))
                                    .assetClass(AssetClass.of(row.getAs("assetClass")))
                                    .spotPrice(spot)
                                    .priceHistory(history)
                                    .build()));
                            return out.iterator();
                        },
                        Encoders.kryo(RiskPosition.class));
    }
}
