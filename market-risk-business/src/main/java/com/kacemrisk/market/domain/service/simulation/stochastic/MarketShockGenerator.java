package com.kacemrisk.market.domain.service.simulation.stochastic;

import com.kacemrisk.market.domain.model.MaturityGrid;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.CholeskyDecomposition;

import java.util.Random;

@Slf4j
@Builder(setterPrefix = "with")
public class MarketShockGenerator {

    private final double @NonNull [][] matrix;
    @NonNull private final MaturityGrid timeGrid;

    @Builder.Default private final int  steps = 10_000;
    @Builder.Default private final long seed  = 42L;

    /**
     * Simulates {@code steps} correlated log-return shocks.
     *
     * <p>The generator is seeded deterministically from {@code seed}, guaranteeing
     * that two calls with the same seed and parameters produce identical shock paths.
     * Use {@code System.nanoTime()} as the seed for independent production runs.
     *
     * @return double[][] of shape [steps][n] containing the total simulated return per risk factor per path.
     */
    public double[][] generateShocks() {
        double[][] L = cholesky(matrix);
        int n = L.length;
        int timeSteps = timeGrid.getSteps();
        double sqrtDt = Math.sqrt(timeGrid.getDt());

        double[][] allPathsShocks = new double[steps][n];

        double[] epsilon   = new double[n];
        double[] stepShocks = new double[n];

        // Seeded RNG — same seed → same paths every run (reproducible integration tests)
        Random rng = new Random(seed);

        log.info("Monte Carlo Engine — paths={}, timeSteps={}, seed={}", steps, timeSteps, seed);

        for (int path = 0; path < steps; path++) {
            for (int t = 0; t < timeSteps; t++) {
                for (int k = 0; k < n; k++) {
                    epsilon[k] = rng.nextGaussian();
                }

                // Correlate shocks via Cholesky factor
                multiplyInPlace(L, epsilon, sqrtDt, stepShocks);

                // Accumulate shocks per path (multi-step geometric Brownian motion)
                for (int k = 0; k < n; k++) {
                    allPathsShocks[path][k] += stepShocks[k];
                }
            }
        }
        return allPathsShocks;
    }

    private static double[][] cholesky(double[][] sigma) {
        return new CholeskyDecomposition(
                new Array2DRowRealMatrix(sigma)).getL().getData();
    }

    private void multiplyInPlace(double[][] L, double[] epsilon, double sqrtDt, double[] result) {
        int n = L.length;
        for (int i = 0; i < n; i++) {
            double sum = 0.0;
            for (int j = 0; j <= i; j++) {
                sum += L[i][j] * epsilon[j];
            }
            result[i] = sum * sqrtDt;
        }
    }
}

