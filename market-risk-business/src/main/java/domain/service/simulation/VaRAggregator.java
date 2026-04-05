package domain.service.simulation;

import domain.model.VaRResult;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;

@Slf4j
public class VaRAggregator {

    private final double[] pnlScenarios;
    private double alpha = 0.99;

    private VaRAggregator(double[] pnlScenarios) {
        this.pnlScenarios = pnlScenarios;
    }

    public static VaRAggregator of(double[] pnlScenarios) {
        return new VaRAggregator(pnlScenarios);
    }

    public VaRAggregator atConfidence(double alpha) {
        this.alpha = alpha;
        return this;
    }

    /**
     * Computes VaR as the (1-α) left-tail quantile of the P&L distribution.
     *
     * Sort ascending → worst losses are at the front.
     * Index = floor((1-α) × N)  e.g. α=0.99, N=10 000 → index 100 (1 % worst).
     * VaR is expressed as a positive loss amount.
     */
    public VaRResult compute() {
        int n = pnlScenarios.length;
        double[] sorted = Arrays.copyOf(pnlScenarios, n);
        Arrays.sort(sorted);

        int idx = (int) Math.floor((1.0 - alpha) * n);
        idx = Math.max(0, Math.min(idx, n - 1));
        double var = -sorted[idx];

        // Expected Shortfall (CVaR) - average of tail losses
        double tailLossesSum = 0.0;
        int tailCount = idx + 1; // losses from index 0 to idx
        for (int i = 0; i <= idx; i++) {
            tailLossesSum += -sorted[i];
        }
        double expectedShortfall = tailCount > 0 ? (tailLossesSum / tailCount) : var;

        double mean = Arrays.stream(sorted).average().orElse(0.0);
        double variance = Arrays.stream(sorted)
                .map(x -> Math.pow(x - mean, 2))
                .sum() / (n - 1);

        log.info("VaRAggregator: α={}, N={}, VaR={}, ES={}", alpha, n, var, expectedShortfall);

        return VaRResult.builder()
                .var(var)
                .expectedShortfall(expectedShortfall)
                .alpha(alpha)
                .numberOfScenarios(n)
                .meanPnL(mean)
                .stdDevPnL(Math.sqrt(variance))
                .build();
    }
}

