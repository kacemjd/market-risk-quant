package com.kacemrisk.market.domain.service.simulation.stochastic;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;
import com.kacemrisk.market.domain.service.calibration.MatrixCalibrator;
import com.kacemrisk.market.domain.service.pricing.PortfolioPricer;
import com.kacemrisk.market.domain.service.simulation.VaRAggregator;
import com.kacemrisk.market.domain.service.simulation.VaRCalculator;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class MonteCarloVaRCalculator implements VaRCalculator {

    @Builder.Default private final int          numPaths = 10_000;
    @Builder.Default private final MaturityGrid timeGrid = MaturityGrid.GRID_53;
    @Builder.Default private final long         seed     = 42L;

    @Override
    public VaRResult calculate(Portfolio portfolio, MarketData marketData, double alpha) {
        log.info("MC VaR | portfolio={} | paths={} | α={}", portfolio.getId(), numPaths, alpha);
        double[][] pathShocks = simulatePaths(marketData);
        double[] pnlScenarios = evaluateScenarios(portfolio, marketData, pathShocks);
        return aggregateVaR(pnlScenarios, alpha);
    }

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

    private double[] evaluateScenarios(Portfolio portfolio, MarketData marketData, double[][] pathShocks) {
        double[] pnlScenarios = new double[numPaths];
        for (int i = 0; i < numPaths; i++) {
            pnlScenarios[i] = PortfolioPricer.price(portfolio, marketData, pathShocks[i]);
        }
        return pnlScenarios;
    }

    private VaRResult aggregateVaR(double[] pnlScenarios, double alpha) {
        return VaRAggregator.of(pnlScenarios, alpha).compute();
    }
}
