package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.infrastructure.model.RiskPositionSlim;
import java.util.Map;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

/**
 * Value object returned by {@link JoinAdapter#enrich}.
 *
 * <p>Owns three Spark-managed resources that must be explicitly released after use:
 *
 * <ol>
 *   <li>{@link #positions()} — lazy {@code Dataset<RiskPositionSlim>} carrying one slim row per
 *       (portfolioId, ticker) pair; no price history embedded
 *   <li>{@link #priceBroadcast()} — the full price history map broadcast to all executors for
 *       driver-side calibration; must be {@code destroy(false)}'d once calibration is complete and
 *       before executor-side VaR computation starts
 *   <li>{@link #portfolioDataset()} — the cached source CSV dataset; must be {@code unpersist()}'d
 *       to release Spark storage memory
 * </ol>
 *
 * <p>Usage in {@code ComposeAdapter}:
 *
 * <pre>{@code
 * Map<String, double[]> allPrices = enriched.priceBroadcast().value(); // calibrate on driver
 * enriched.release();                                                   // free before VaR starts
 * Broadcast<MarketData> mdBroadcast = jsc.broadcast(marketData);
 * try {
 *     results = enriched.positions().groupByKey(...).mapGroups(...).collectAsList();
 * } finally {
 *     mdBroadcast.destroy(false);
 * }
 * }</pre>
 */
public record EnrichedDataset(
        Dataset<RiskPositionSlim> positions,
        Broadcast<Map<String, double[]>> priceBroadcast,
        Dataset<Row> portfolioDataset) {
    /**
     * Releases all Spark-managed resources. Safe to call in a {@code finally} block — tolerates
     * {@code null} references. {@code Broadcast.destroy(false)} removes the broadcast value from
     * Spark's block manager without blocking executor confirmation.
     */
    public void release() {
        if (priceBroadcast != null) {
            priceBroadcast.destroy(false);
        }
        if (portfolioDataset != null) {
            portfolioDataset.unpersist();
        }
    }
}
