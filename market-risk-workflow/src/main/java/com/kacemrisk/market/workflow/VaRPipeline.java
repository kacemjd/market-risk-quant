package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;

/**
 * Framework-free port — executes VaR computation for a single portfolio.
 *
 * <p>Calibration has been moved out of this pipeline and is performed once at
 * scenario level by the caller ({@code ComposeAdapter}) before iterating over
 * portfolio groups. The pre-calibrated {@link MarketData} is passed in here so
 * the pipeline only needs to run the VaR calculation step.
 */
public interface VaRPipeline {

    /**
     * Computes VaR for a single portfolio using pre-calibrated market data.
     *
     * @param portfolio  domain portfolio for this group
     * @param marketData calibrated market data (volatilities, covariance, returns) — produced
     *                   once per scenario run, shared across all portfolio groups
     * @param ctx        execution metadata (method, confidence, window, …)
     * @return {@link VaROutput} carrying the portfolio, market data, and VaR result
     */
    VaROutput execute(Portfolio portfolio, MarketData marketData, RunContext ctx);
}
