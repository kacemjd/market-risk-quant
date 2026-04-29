package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.infrastructure.model.RiskPosition;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;

import java.util.Map;

/**
 * Value object returned by {@link JoinAdapter#enrich}.
 *
 * <p>Carries the three Spark-managed resources that must be explicitly released
 * after the driver-side {@code collectAsList()} call in {@link ComposeAdapter}:
 * <ol>
 *   <li>{@link #positions()} — the enriched lazy {@code Dataset<RiskPosition>}</li>
 *   <li>{@link #priceBroadcast()} — the distributed price map; must be
 *       {@code destroy(false)}'d to free executor memory (Phase 2B, C3 fix)</li>
 *   <li>{@link #portfolioDataset()} — the cached source CSV dataset; must be
 *       {@code unpersist()}'d to release Spark storage memory (Phase 2B, C2 fix)</li>
 * </ol>
 *
 * <p>Usage in {@code ComposeAdapter}:
 * <pre>{@code
 * List<RiskPosition> positions;
 * try {
 *     positions = enriched.positions().collectAsList();
 * } finally {
 *     enriched.release();   // destroys broadcast + unpersists portfolio dataset
 * }
 * }</pre>
 */
public record EnrichedDataset(
        Dataset<RiskPosition> positions,
        Broadcast<Map<String, double[]>> priceBroadcast,
        Dataset<Row> portfolioDataset
) {
    /**
     * Releases all Spark-managed resources held by this enriched dataset.
     *
     * <p>Safe to call in a {@code finally} block — tolerates {@code null} references.
     * {@code Broadcast.destroy(false)} removes the value from Spark's block manager
     * without blocking until all executors confirm removal.
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

