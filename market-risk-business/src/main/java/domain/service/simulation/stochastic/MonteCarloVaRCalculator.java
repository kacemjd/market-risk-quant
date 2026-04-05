package domain.service.simulation.stochastic;

import domain.model.MarketData;
import domain.model.MaturityGrid;
import domain.model.Portoflio;
import domain.model.VaRResult;
import domain.service.calibration.MatrixCalibrator;
import domain.service.pricing.PortfolioPricer;
import domain.service.simulation.VaRAggregator;
import domain.service.simulation.VaRCalculator;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

/**
 * Monte Carlo VaR — composes {@link MarketShockGenerator} (scenario generation)
 * and {@link VaRAggregator} (quantile extraction) behind the unified
 * {@link VaRCalculator} interface.
 *
 * <p>Algorithm:
 * <ol>
 *   <li>Generate N correlated path shocks via Cholesky-decomposed GBM.</li>
 *   <li>Price the portfolio under each shock scenario via the {@link PortfolioPricer}.</li>
 *   <li>Extract the (1 − α) empirical quantile and Expected Shortfall from the simulated distribution.</li>
 * </ol>
 */
@Slf4j
@Builder
public class MonteCarloVaRCalculator implements VaRCalculator {

    @Builder.Default private final int          numPaths = 10_000;
    @Builder.Default private final MaturityGrid timeGrid = MaturityGrid.GRID_53;
    @Builder.Default private final long         seed     = 42L;

    @Override
    public VaRResult calculate(Portoflio portoflio, MarketData marketData, double alpha) {
        log.info("MC VaR | portfolio={} | paths={} | α={}", portoflio.getId(), numPaths, alpha);
        double[][] pathShocks = simulatePaths(marketData);
        double[] pnlScenarios = evaluateScenarios(portoflio, marketData, pathShocks);
        return aggregateVaR(pnlScenarios, alpha);
    }

    /**
     * Generates {@code numPaths} raw log-return shock paths via {@link MarketShockGenerator}.
     *
     * @return double[numPaths][n] representing cumulative log-return shock per path
     */
    private double[][] simulatePaths(MarketData marketData) {
        double[][] sigma = MatrixCalibrator.from(marketData).calculateSigma();

        return MarketShockGenerator.builder()
                .withMatrix(sigma)
                .withSteps(numPaths)
                .withTimeGrid(timeGrid)
                .withSeed(seed)
                .build()
                .generateShocks();
    }

    private double[] evaluateScenarios(Portoflio portfolio, MarketData marketData, double[][] pathShocks) {
        double[] pnlScenarios = new double[numPaths];
        for (int i = 0; i < numPaths; i++) {
            pnlScenarios[i] = PortfolioPricer.price(portfolio, marketData, pathShocks[i]);
        }
        return pnlScenarios;
    }

    private VaRResult aggregateVaR(double[] pnlScenarios, double alpha) {
        return VaRAggregator.of(pnlScenarios)
                .atConfidence(alpha)
                .compute();
    }
}
