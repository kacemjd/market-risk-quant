package com.kacemrisk.market.infrastructure.model;

import com.kacemrisk.market.domain.model.AssetClass;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * The single enriched unit the risk engine operates on.
 *
 * <p>Carries everything needed for one (portfolioId, ticker) pair:
 * portfolio membership, market exposure, and a primitive price history.
 *
 * <p>{@code priceHistory} is a contiguous {@code double[]} (oldest → newest) so the
 * JVM can vectorise calibration and VaR loops without per-element object dereferencing.
 * Dates are consumed at load time (sorting + window filtering) and are irrelevant to the math.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskPosition implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String portfolioId;
    private String ticker;
    private double quantity;
    /**
     * Compile-time–safe asset class — converted from String at the Spark boundary.
     */
    private AssetClass assetClass;
    /**
     * Latest close price as of the run's asOfDate.
     */
    private double spotPrice;
    /**
     * Close prices sorted oldest → newest; primitive array for zero boxing.
     */
    private double[] priceHistory;
}

