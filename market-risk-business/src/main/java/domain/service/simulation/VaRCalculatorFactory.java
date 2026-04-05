package domain.service.simulation;

import domain.model.MaturityGrid;
import domain.model.VaRMethod;
import domain.service.simulation.analytical.ParametricVaRCalculator;
import domain.service.simulation.stochastic.MonteCarloVaRCalculator;
import domain.service.simulation.historical.HistoricalVaRCalculator;

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
