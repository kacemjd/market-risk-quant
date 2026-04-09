package com.kacemrisk.market.domain.service.pricing;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;

/**
 * Orchestrates the application of scenario shocks on a whole portfolio via {@link PricerFactory}.
 */
public class PortfolioPricer {

    private PortfolioPricer() {}

    /**
     * Compute total portfolio P&L given an array of raw log-returns (shocks).
     * The shocks array must align conceptually with marketData's riskFactors order.
     */
    public static double price(Portfolio portfolio, MarketData marketData, double[] shocks) {
        double portfolioPnl = 0.0;

        for (int i = 0; i < marketData.getRiskFactors().size(); i++) {
            String ticker = marketData.getRiskFactors().get(i);
            double shock = shocks[i];

            if (shock == 0.0) continue;

            portfolioPnl += portfolio.getPositions().stream()
                    .filter(p -> p.getTicker().equals(ticker))
                    .mapToDouble(p -> PricerFactory.getPricerFor(p).calculatePnl(p, shock))
                    .sum();
        }

        return portfolioPnl;
    }
}

