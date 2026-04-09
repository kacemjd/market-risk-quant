package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;
import com.kacemrisk.market.domain.service.simulation.stochastic.MonteCarloVaRCalculator;

/**
 * Concrete VaR pipeline that always executes Monte Carlo simulation,
 * bypassing the method-dispatch logic in {@link VaRCalculationPipeline}.
 *
 * <p>Prefer {@link VaRCalculationPipeline} for runtime method selection
 * (PARAMETRIC / MONTE_CARLO / HISTORICAL). Use this pipeline only when
 * Monte Carlo is unconditionally required regardless of the notification's
 * {@code varMethod} field.
 */
public class MonteCarloVaRPipeline implements VaRPipeline {

    @Override
    public VaRResult execute(Portfolio portfolio, MarketData marketData, ScenarioNotification notification) {
        return MonteCarloVaRCalculator.builder()
                .numPaths(notification.getNumPaths())
                .timeGrid(notification.getTimeGrid())
                .build()
                .calculate(portfolio, marketData, notification.getConfidenceLevel());
    }
}

