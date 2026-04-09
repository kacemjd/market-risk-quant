package com.kacemrisk.market.domain.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Position {

    String ticker;
    AssetClass assetClass;
    double quantity;
    double spotPrice;

    /**
     * Unit price sensitivity (Δ): P&L change per $1 move in the underlying.
     * Pre-computed by the pricing engine and stored here so the risk calculator
     * never needs to re-price.
     * <ul>
     *   <li>Equity spot / future: always 1.0</li>
     *   <li>Call option: (0, 1) — typically ~0.5 at-the-money</li>
     *   <li>Put option: (-1, 0)</li>
     * </ul>
     * Dollar delta = quantity × spotPrice × delta.
     */
    double delta;

    /**
     * Second-order price sensitivity (Γ): rate of change of delta per $1 move.
     * Zero for linear instruments (equities, futures).
     */
    double gamma;

    double maturityInYears;

    public double getNotional() {
        return quantity * spotPrice;
    }

    /**
     * Factory for a plain equity spot position.
     * Delta is 1.0 by definition — the position value moves 1:1 with the spot price.
     */
    public static Position equitySpot(String ticker, double quantity, double spotPrice) {
        return Position.builder()
                .ticker(ticker)
                .assetClass(AssetClass.EQD)
                .quantity(quantity)
                .spotPrice(spotPrice)
                .delta(1.0)
                .gamma(0.0)
                .maturityInYears(0.0)
                .build();
    }
}
