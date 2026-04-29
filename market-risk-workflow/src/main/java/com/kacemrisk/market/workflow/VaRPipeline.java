package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.Portfolio;

import java.util.Map;

public interface VaRPipeline {

    /**
     * Executes calibration + VaR computation for a single portfolio group.
     *
     * @param portfolio       domain portfolio built from one portfolioId group
     * @param pricesByTicker  price histories for every ticker in the portfolio (oldest→newest)
     * @param ctx             execution metadata (method, confidence, window, …)
     * @return {@link VaROutput} carrying the calibrated {@code MarketData} and {@code VaRResult}
     */
    VaROutput execute(Portfolio portfolio, Map<String, double[]> pricesByTicker, RunContext ctx);
}
