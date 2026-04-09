package com.kacemrisk.market.domain.service.simulation;

import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.domain.service.simulation.analytical.ParametricVaRCalculator;
import com.kacemrisk.market.domain.service.simulation.stochastic.MonteCarloVaRCalculator;
import com.kacemrisk.market.domain.service.simulation.historical.HistoricalVaRCalculator;

/**
 * Domain Factory — Encapsulates the knowledge of how to build and configure
 * concrete {@link VaRCalculator} strategies.
 */
public class VaRCalculatorFactory {

    private VaRCalculatorFactory() {}

    /**
     * Builds the appropriate VaRCalculator strategy based on the given method.
     */
    public static VaRCalculator create(VaRMethod method, int numPaths, MaturityGrid grid, int historicalWindow) {
        return switch (method) {
            case PARAMETRIC -> new ParametricVaRCalculator();
            case MONTE_CARLO -> MonteCarloVaRCalculator.builder()
                    .numPaths(numPaths)
                    .timeGrid(grid)
                    .build();
            case HISTORICAL -> new HistoricalVaRCalculator(historicalWindow);
        };
    }
}
