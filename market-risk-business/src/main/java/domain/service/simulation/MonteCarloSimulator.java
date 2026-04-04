package domain.service.simulation;

import domain.model.MaturityGrid;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;

import java.util.Random;

@Slf4j
@Builder(setterPrefix = "with")
public class MonteCarloSimulator {

    @NonNull private final double[][] matrix;
    @NonNull private final double[]   deltas;
    @NonNull private final MaturityGrid timeGrid;

    @Builder.Default private final int  steps = 10_000;
    @Builder.Default private final long seed  = 42L;

    /**
     * Simulates {@code steps} correlated P&L paths over the {@code timeGrid}.
     *
     * Algorithm per path:
     *   for each time step t:
     *     ε ~ N(0,I)^n
     *     r_t = L × ε × √(dt)          correlated log-returns
     *     P&L += δᵀ × r_t              first-order linear approximation
     *
     * @return array of length {@code steps}, each entry the simulated portfolio P&L
     */
    public double[] generatePaths() {
        double[][] L = cholesky(matrix);
        int n = L.length;
        int timeSteps = timeGrid.getSteps();
        double sqrtDt = Math.sqrt(timeGrid.getDt());

        double[] pnl = new double[steps];
        Random rng = new Random(seed);

        log.info("Monte Carlo: {} paths × {} time steps × {} risk factors", steps, timeSteps, n);

        for (int path = 0; path < steps; path++) {
            double pathPnL = 0.0;
            for (int t = 0; t < timeSteps; t++) {
                double[] epsilon = new double[n];
                for (int k = 0; k < n; k++) {
                    epsilon[k] = rng.nextGaussian();
                }
                double[] r = multiply(L, epsilon, sqrtDt);
                pathPnL += dot(deltas, r);
            }
            pnl[path] = pathPnL;
        }

        log.info("Monte Carlo: path generation complete");
        return pnl;
    }

    private static double[][] cholesky(double[][] sigma) {
        return new CholeskyDecomposition(
                new Array2DRowRealMatrix(sigma)).getL().getData();
    }

    /** r_i = sqrtDt × Σ_j L_ij × ε_j */
    private static double[] multiply(double[][] L, double[] epsilon, double scale) {
        int n = L.length;
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j <= i; j++) {
                sum += L[i][j] * epsilon[j];
            }
            result[i] = scale * sum;
        }
        return result;
    }

    /** δᵀ × r */
    private static double dot(double[] delta, double[] r) {
        double sum = 0.0;
        for (int i = 0; i < delta.length; i++) {
            sum += delta[i] * r[i];
        }
        return sum;
    }
}

