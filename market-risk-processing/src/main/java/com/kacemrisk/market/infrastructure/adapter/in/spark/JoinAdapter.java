package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.infrastructure.loader.MarketPriceService;
import com.kacemrisk.market.infrastructure.loader.PortfolioLoader;
import com.kacemrisk.market.infrastructure.model.RiskModelReferential;
import com.kacemrisk.market.infrastructure.model.RiskPosition;
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
import com.kacemrisk.market.workflow.RunContext;

import java.time.LocalDate;
import java.util.*;

/**
 * Spark-native enrichment pipeline.
 *
 * <h3>Phase A — portfolio load</h3>
 * Delegates to {@link PortfolioLoader}: {@code spark.read().csv(configuredPath)},
 * normalised + deduplicated inside Spark.  One {@code collectAsList()} extracts the
 * {@code ticker → AssetClass} map needed by Phase B; the cached {@link Dataset} is
 * reused in Phase C without a second parse pass.
 *
 * <h3>Phase B — price fetch</h3>
 * Delegates to {@link MarketPriceService}: calls the configured
 * {@link com.kacemrisk.market.infrastructure.loader.HistoricalPriceLoader} strategy and
 * returns {@code Map<String, double[]>} (sorted oldest → newest; spot = {@code array[last]}).
 * The map is <b>broadcast</b> so every executor receives a single efficient copy.
 *
 * <h3>Phase C — Spark enrichment via flatMapGroups</h3>
 * Groups the portfolio dataset by ticker, streams over each group with
 * {@code flatMapGroups}, and looks up the broadcast price array to build one
 * {@link RiskPosition} per (portfolioId, ticker) row — directly, with no intermediate
 * bean schema.  Encoders.kryo is used because {@link RiskPosition} carries a
 * primitive {@code double[]} and an enum, which are outside the standard bean encoder.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JoinAdapter {

    private final SparkSession spark;
    private final PortfolioLoader portfolioLoader;
    private final MarketPriceService marketPriceService;

    /**
     * Runs Phases A → B → C and returns the fully-enriched referential.
     *
     * @param ctx execution context — provides {@code asOfDate} and {@code historicalWindow}
     * @return {@link RiskModelReferential} keyed by (portfolioId, ticker)
     */
    public RiskModelReferential enrich(RunContext ctx) {

        Dataset<Row> portfolioDs = portfolioLoader.load();

        // ── Phase A — validate + extract unique tickers ───────────────────────────────────
        // select distinct over two small string columns — tiny result, cheap job.
        // Avoids collectAsList() on the full portfolio dataset.
        if (portfolioDs.isEmpty()) {
            log.error("[JoinAdapter] Portfolio dataset is empty — aborting.");
            return new RiskModelReferential(Collections.emptyList());
        }

        List<Row> tickerRows = portfolioDs
                .select("ticker", "assetClass")
                .distinct()
                .collectAsList();   // ← small: one row per unique ticker

        Map<String, AssetClass> tickerAssetClass = new LinkedHashMap<>();
        for (Row r : tickerRows) {
            try {
                tickerAssetClass.putIfAbsent(r.getString(0), AssetClass.valueOf(r.getString(1)));
            } catch (IllegalArgumentException ex) {
                log.warn("[JoinAdapter] Unknown asset class '{}' for ticker '{}' — skipping",
                        r.getString(1), r.getString(0));
            }
        }
        log.info("[JoinAdapter] Phase A — {} unique ticker(s)", tickerAssetClass.size());

        // ── Phase B — price fetch + broadcast ────────────────────────────────────────────
        // Calendar buffer: 2 × window → absorbs weekends + public holidays
        LocalDate from = ctx.asOfDate().minusDays(ctx.historicalWindow() * 2L);
        Map<String, double[]> pricesByTicker = marketPriceService.loadPrices(
                tickerAssetClass, from, ctx.asOfDate());
        if (pricesByTicker.isEmpty()) {
            log.error("[JoinAdapter] No prices loaded for window [{} → {}] — aborting.",
                    from, ctx.asOfDate());
            return new RiskModelReferential(Collections.emptyList());
        }

        Broadcast<Map<String, double[]>> priceBroadcast =
                JavaSparkContext.fromSparkContext(spark.sparkContext()).broadcast(pricesByTicker);

        // ── Phase C — stream by ticker group, enrich via broadcast, exit Spark once ───────
        // groupByKey(ticker): price array looked up O(1) per group, not per row.
        // flatMapGroups: one RiskPosition emitted per (portfolioId, ticker) row.
        // collectAsList(): the single Spark exit boundary for this pipeline.
        List<RiskPosition> positions = portfolioDs
                .groupByKey(
                        (MapFunction<Row, String>) row -> row.getString(row.fieldIndex("ticker")),
                        Encoders.STRING())
                .flatMapGroups(
                        (FlatMapGroupsFunction<String, Row, RiskPosition>) (ticker, rowIter) -> {
                            double[] history = priceBroadcast.value().getOrDefault(ticker, new double[0]);
                            double   spot    = history.length > 0 ? history[history.length - 1] : 0.0;
                            List<RiskPosition> out = new ArrayList<>();
                            rowIter.forEachRemaining(row -> out.add(RiskPosition.builder()
                                    .portfolioId(row.getString(row.fieldIndex("portfolioId")))
                                    .ticker(ticker)
                                    .quantity(row.getDouble(row.fieldIndex("quantity")))
                                    .assetClass(AssetClass.valueOf(row.getString(row.fieldIndex("assetClass"))))
                                    .spotPrice(spot)
                                    .priceHistory(history)
                                    .build()));
                            return out.iterator();
                        },
                        Encoders.kryo(RiskPosition.class))
                .collectAsList();   // ← single Spark exit boundary

        priceBroadcast.unpersist();
        log.info("[JoinAdapter] Referential built — {} position(s)", positions.size());
        return new RiskModelReferential(positions);
    }
}
