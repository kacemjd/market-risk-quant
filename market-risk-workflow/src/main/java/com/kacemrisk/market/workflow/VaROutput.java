package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;

/**
 * Carries the full output of one portfolio's VaR pipeline run.
 *
 * <p>Returned by {@link VaRPipeline#execute} so that {@code ComposeAdapter}
 * has everything it needs to persist calibration data and publish results
 * without knowing pipeline internals.
 */
public record VaROutput(Portfolio portfolio, MarketData marketData, VaRResult varResult) {}

