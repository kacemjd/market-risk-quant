package domain.service.calibration;

import domain.model.MarketData;
import domain.model.Portoflio;

import java.util.List;

public class MatrixCalibrator {

    private final MarketData marketData;

    private MatrixCalibrator(MarketData marketData) {
        this.marketData = marketData;
    }

    public static MatrixCalibrator from(MarketData marketData) {
        return new MatrixCalibrator(marketData);
    }

    /**
     * Re-derives the covariance matrix from the stored volatilities and correlation matrix.
     * Σ_ij = ρ_ij × σ_i × σ_j
     */
    public double[][] calculateSigma() {
        List<String> factors = marketData.getRiskFactors();
        int n = factors.size();
        double[][] rho = marketData.getCorrelationMatrix();
        double[][] sigma = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                sigma[i][j] = rho[i][j]
                        * marketData.getVolFor(factors.get(i))
                        * marketData.getVolFor(factors.get(j));
            }
        }
        return sigma;
    }


    /**
     * Extracts the dollar-delta vector aligned with {@link MarketData#getRiskFactors()}.
     * dollarDelta_i = Σ_positions(quantity × spotPrice × delta) for ticker i.
     */
    public double[] extractDeltas(Portoflio portfolio) {
        return marketData.getRiskFactors().stream()
                .mapToDouble(ticker -> portfolio.getPositions().stream()
                        .filter(p -> p.getTicker().equals(ticker))
                        .mapToDouble(p -> p.getQuantity() * p.getSpotPrice() * p.getDelta())
                        .sum())
                .toArray();
    }
}

