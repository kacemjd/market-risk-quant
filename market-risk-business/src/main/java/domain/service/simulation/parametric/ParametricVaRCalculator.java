package domain.service.simulation;

import domain.model.MarketData;
import domain.model.Portoflio;
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
    public double calculate(Portoflio portoflio, MarketData marketData, double alpha) {
        double portfolioVariance = calculatePortfolioVariance(portoflio, marketData);
        double portfolioStdDev = sqrt(portfolioVariance);
        double zScore = new NormalDistribution(0, 1).inverseCumulativeProbability(alpha);
        double valueAtRisk = zScore * portfolioStdDev;
        log.debug("Portfolio variance = {}, stdDev = {}, z({}) = {}, VaR = {}", portfolioVariance, portfolioStdDev, alpha, zScore, valueAtRisk);
        return valueAtRisk;
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

    static double computeVarianceLoop(double[] deltas, double[][] sigma) {
        int n = deltas.length;
        double variance = 0.0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                variance += deltas[i] * sigma[i][j] * deltas[j];
            }
        }
        return variance;
    }

    static double computeVarianceMatrix(double[] deltas, double[][] matrix) {
        RealVector delta = new ArrayRealVector(deltas);
        RealMatrix sigma = new Array2DRowRealMatrix(matrix);
        return delta.dotProduct(sigma.operate(delta));
    }
}
