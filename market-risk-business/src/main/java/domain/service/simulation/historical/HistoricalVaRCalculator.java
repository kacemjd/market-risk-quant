package domain.service.simulation.historical;

import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.VaRResult;
import domain.service.pricing.PortfolioPricer;
import domain.service.simulation.VaRAggregator;
import domain.service.simulation.VaRCalculator;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Full-revaluation Historical Simulation VaR.
 */
@Slf4j
public class HistoricalVaRCalculator implements VaRCalculator {

    private final int windowSize;

    public HistoricalVaRCalculator(int windowSize) {
        this.windowSize = windowSize;
    }

    @Override
    public VaRResult calculate(Portoflio portoflio, MarketData marketData, double alpha) {
        log.info("Historical VaR | portfolio={} | window={} days | α={}",
                portoflio.getId(), windowSize, alpha);

        Map<String, double[]> historicalReturnsMap = marketData.getHistoricalReturns();

        if (historicalReturnsMap == null || historicalReturnsMap.isEmpty()) {
            throw new IllegalStateException("MarketData is missing historical returns map needed for HISTORICAL VaR");
        }

        double[] scenarioPnLs = replayHistoricalScenarios(portoflio, marketData, historicalReturnsMap);
        return extractQuantile(scenarioPnLs, alpha);
    }


    double[] replayHistoricalScenarios(Portoflio portfolio, MarketData marketData, Map<String, double[]> historicalReturnsMap) {
        List<String> riskFactors = marketData.getRiskFactors();

        riskFactors.forEach(ticker -> {
            double[] returns = historicalReturnsMap.get(ticker);
            if (returns == null) {
                log.warn("No historical returns found for risk factor '{}' — contributing zero P&L", ticker);
            } else if (returns.length < windowSize) {
                log.warn("Risk factor '{}' has only {} days of history — requested window is {} days. " +
                         "VaR will be computed on the available {} days for this factor.",
                        ticker, returns.length, windowSize, returns.length);
            }
        });

        double[] scenarioPnLs = new double[windowSize];
        double[] currentShocks = new double[riskFactors.size()];

        for (int t = 0; t < windowSize; t++) {
            for (int i = 0; i < riskFactors.size(); i++) {
                double[] returns = historicalReturnsMap.get(riskFactors.get(i));
                if (returns != null && returns.length > 0) {
                    int idx = Math.max(0, returns.length - windowSize + t);
                    currentShocks[i] = returns[idx];
                } else {
                    currentShocks[i] = 0.0;
                }
            }
            scenarioPnLs[t] = PortfolioPricer.price(portfolio, marketData, currentShocks);
        }
        return scenarioPnLs;
    }

    /**
     * Delegates to {@link VaRAggregator} to extract the (1 − α) left-tail quantile
     * from the empirical P&L distribution.
     *
     * @return full {@link VaRResult} including mean, stdDev and scenario count
     */
    VaRResult extractQuantile(double[] scenarioPnLs, double alpha) {
        return VaRAggregator.of(scenarioPnLs)
                .atConfidence(alpha)
                .compute();
    }
}
