package com.kacemrisk.market.domain.service.simulation.analytical;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.Position;
import com.kacemrisk.market.domain.model.VaRResult;
import com.kacemrisk.market.domain.service.simulation.VaRCalculator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.sqrt;

@Slf4j
public class ParametricVaRCalculator implements VaRCalculator {

    /**
     * Standard normal distribution — stateless and thread-safe, shared across all portfolios
     * to avoid per-call object allocation.
     */
    private static final NormalDistribution STANDARD_NORMAL = new NormalDistribution(0, 1);

    @Override
    public VaRResult calculate(Portfolio portfolio, MarketData marketData, double alpha) {

        double portfolioVariance = calculatePortfolioVariance(portfolio, marketData);
        double portfolioStdDev = sqrt(portfolioVariance);
        double zScore = STANDARD_NORMAL.inverseCumulativeProbability(alpha);
        double valueAtRisk = zScore * portfolioStdDev;

        // Closed-form Gaussian ES: ES_α = σ × φ(Φ⁻¹(α)) / (1 − α)
        // where φ is the standard normal PDF and Φ⁻¹ is the inverse CDF.
        double expectedShortfall = portfolioStdDev * STANDARD_NORMAL.density(zScore) / (1.0 - alpha);

        log.debug("Portfolio stdDev={}, z({})={}, VaR={}, ES={}", portfolioStdDev, alpha, zScore, valueAtRisk, expectedShortfall);

        return VaRResult.builder()
                .var(valueAtRisk)
                .expectedShortfall(expectedShortfall)
                .alpha(alpha)
                .numberOfScenarios(0)   // closed-form — no simulation scenarios
                .meanPnL(0.0)           // zero-mean P&L assumption under Gaussian
                .stdDevPnL(portfolioStdDev)
                .build();
    }

    public double calculatePortfolioVariance(Portfolio portfolio, MarketData marketData) {
        // Pre-compute dollar-delta map once — O(|positions|) — rather than scanning all positions
        // for every risk factor in the covariance matrix loop.
        Map<String, Double> dollarDeltaByTicker = portfolio.getPositions().stream()
                .collect(Collectors.toMap(
                        Position::getTicker,
                        p -> p.getQuantity() * p.getSpotPrice() * p.getDelta(),
                        Double::sum));

        double[] deltas = marketData.getRiskFactors().stream()
                .mapToDouble(t -> dollarDeltaByTicker.getOrDefault(t, 0.0))
                .toArray();
        return computeVarianceLoop(deltas, marketData.getCovarianceMatrix());
    }


    public static double computeVarianceLoop(double[] deltas, double[][] sigma) {
        int n = deltas.length;
        double variance = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                variance += deltas[i] * sigma[i][j] * deltas[j];
            }
        }
        return variance;
    }

    public static double computeVarianceMatrix(double[] deltas, double[][] matrix) {
        RealVector delta = new ArrayRealVector(deltas);
        RealMatrix sigma = new Array2DRowRealMatrix(matrix);
        return delta.dotProduct(sigma.operate(delta));
    }
}
