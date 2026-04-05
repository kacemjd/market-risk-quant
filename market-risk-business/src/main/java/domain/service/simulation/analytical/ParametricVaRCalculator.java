package domain.service.simulation.analytical;

import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.VaRResult;
import domain.service.simulation.VaRCalculator;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;

import static java.lang.Math.sqrt;

@Slf4j
public class ParametricVaRCalculator implements VaRCalculator {

    @Override
    public VaRResult calculate(Portoflio portoflio, MarketData marketData, double alpha) {
        double portfolioVariance = calculatePortfolioVariance(portoflio, marketData);
        double portfolioStdDev = sqrt(portfolioVariance);

        NormalDistribution normal = new NormalDistribution(0, 1);
        double zScore = normal.inverseCumulativeProbability(alpha);
        double valueAtRisk = zScore * portfolioStdDev;

        // Closed-form Gaussian ES: ES_α = σ × φ(Φ⁻¹(α)) / (1 − α)
        // where φ is the standard normal PDF and Φ⁻¹ is the inverse CDF.
        double expectedShortfall = portfolioStdDev * normal.density(zScore) / (1.0 - alpha);

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

    public double calculatePortfolioVariance(Portoflio portoflio, MarketData marketData) {
        double[] deltas = marketData.getRiskFactors().stream()
                .mapToDouble(ticker -> findDeltaForTicker(portoflio, ticker))
                .toArray();
        return computeVarianceLoop(deltas, marketData.getCovarianceMatrix());
    }

    private double findDeltaForTicker(Portoflio portoflio, String ticker) {
        return portoflio.getPositions().stream()
                .filter(p -> p.getTicker().equals(ticker))
                .mapToDouble(p -> p.getQuantity() * p.getSpotPrice() * p.getDelta())
                .sum();
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
