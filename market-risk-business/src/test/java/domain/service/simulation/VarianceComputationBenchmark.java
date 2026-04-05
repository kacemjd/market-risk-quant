package domain.service.simulation;

import domain.service.simulation.analytical.ParametricVaRCalculator;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Benchmark)
public class VarianceComputationBenchmark {

    @Param({"5", "50", "500"})
    int n;

    double[] deltas;
    double[][] covarianceMatrix;

    @Setup(Level.Trial)
    public void setup() {
        Random rng = new Random(42);
        deltas = new double[n];
        for (int i = 0; i < n; i++) {
            deltas[i] = rng.nextDouble();
        }

        double[][] a = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                a[i][j] = rng.nextDouble();
            }
        }
        covarianceMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double sum = 0.0;
                for (int k = 0; k < n; k++) {
                    sum += a[k][i] * a[k][j];
                }
                covarianceMatrix[i][j] = sum;
            }
        }
    }

    @Benchmark
    public double loop() {
        return ParametricVaRCalculator.computeVarianceLoop(deltas, covarianceMatrix);
    }

    @Benchmark
    public double commonsMatrix() {
        return ParametricVaRCalculator.computeVarianceMatrix(deltas, covarianceMatrix);
    }

    public static void main(String[] args) throws RunnerException {
        Options opts = new OptionsBuilder()
                .include(VarianceComputationBenchmark.class.getSimpleName())
                .build();
        new Runner(opts).run();
    }
}

