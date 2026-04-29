package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
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

/**
 * Spark enrichment adapter — joins portfolio positions with historical market prices
 * and produces an enriched {@link Dataset} of {@link RiskPosition}s ready for VaR.
 *
 * <h3>Phase execution</h3>
 * <ol>
 *   <li><b>Phase A</b> — collect unique {@code ticker → assetClass} from the cached portfolio dataset</li>
 *   <li><b>Phase B</b> — bulk-fetch prices via {@link FetchHistoricalPricesUseCase#fetch},
 *       serialise {@code List<HistoricalPrice>} to {@code Map<ticker, double[]>} (sorted oldest→newest),
 *       then broadcast to all executors</li>
 *   <li><b>Phase C</b> — {@code groupByKey(ticker) → flatMapGroups} to build one
 *       {@link RiskPosition} per (portfolioId, ticker) pair</li>
 * </ol>
 *
 * <h3>Lifecycle</h3>
 * The returned {@link EnrichedDataset} carries the broadcast and the cached portfolio dataset.
 * The caller ({@code ComposeAdapter}) <b>must</b> call {@link EnrichedDataset#release()} in
 * a {@code finally} block after {@code collectAsList()} to prevent per-scenario Spark memory leaks.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JoinAdapter {

    private final SparkSession spark;
    private final PortfolioLoader portfolioLoader;
    private final FetchHistoricalPricesUseCase fetchHistoricalPricesUseCase;

    /**
     * Enriches the portfolio with historical prices and returns a lazy {@link EnrichedDataset}.
     *
     * <p>The portfolio dataset is cached for Phase A re-use by Phase C. Both the cache
     * and the price broadcast are owned by the returned {@link EnrichedDataset} and
     * released by the caller via {@link EnrichedDataset#release()}.
     */
    public EnrichedDataset enrich(RunContext ctx) {

        Dataset<Row> portfolioDs = portfolioLoader.load().cache();

        if (portfolioDs.isEmpty()) {
            log.error("[JoinAdapter] Portfolio dataset is empty — aborting.");
            portfolioDs.unpersist();
            return new EnrichedDataset(
                    spark.emptyDataset(Encoders.kryo(RiskPosition.class)), null, null);
        }

        // ── Phase A — collect unique ticker→assetClass (cache hit avoids second CSV read) ──
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

        // ── Phase B — bulk fetch + serialise to Map<ticker, double[]> ────────────────────
        var from = ctx.asOfDate().minusDays(ctx.historicalWindow());

        List<HistoricalPrice> rawPrices = fetchHistoricalPricesUseCase.fetch(
                FetchHistoricalPricesCommand.builder()
                        .tickers(tickerAssetClass)
                        .from(from)
                        .to(ctx.asOfDate())
                        .build());

        if (rawPrices.isEmpty()) {
            log.error("[JoinAdapter] No prices loaded for window [{} → {}] — aborting.", from, ctx.asOfDate());
            portfolioDs.unpersist();
            return new EnrichedDataset(
                    spark.emptyDataset(Encoders.kryo(RiskPosition.class)), null, null);
        }

        // Serialise List<HistoricalPrice> → Map<ticker, double[]> sorted oldest→newest
        Map<String, double[]> pricesByTicker = rawPrices.stream()
                .collect(Collectors.groupingBy(
                        HistoricalPrice::getTicker,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(HistoricalPrice::getDate))
                                        .mapToDouble(HistoricalPrice::getClosePrice)
                                        .toArray())));

        log.info("[JoinAdapter] Phase B — {} ticker(s) serialised, {} total price records",
                pricesByTicker.size(), rawPrices.size());

        Broadcast<Map<String, double[]>> priceBroadcast =
                JavaSparkContext.fromSparkContext(spark.sparkContext()).broadcast(pricesByTicker);

        // ── Phase C — enrich via broadcast, return lazy Dataset ──────────────────────────
        // Caller owns release(): priceBroadcast.destroy() + portfolioDs.unpersist()
        Dataset<RiskPosition> positions = portfolioDs
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

        return new EnrichedDataset(positions, priceBroadcast, portfolioDs);
    }
}
