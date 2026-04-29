package com.kacemrisk.market.domain.service.calibration;

import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.domain.model.MarketData;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

/**
 * Domain service that calibrates market data (volatilities, correlations, covariance)
 * from a map of historical closing price series.
 *
 * <p>Implements {@link CalibrateMarketDataUseCase} directly — no passthrough wrapper needed.
 *
 * <p><b>Resilience contract:</b> tickers with fewer than 2 price observations are silently
 * skipped (logged at WARN level). The returned {@link MarketData} covers only the surviving
 * tickers. Callers must handle the case where calibration returns fewer risk factors than
 * the original universe.
 */
@Slf4j
public class MarketDataCalibrationService implements CalibrateMarketDataUseCase {

    private static final int DAYS_IN_YEAR = 252;

    /**
     * {@inheritDoc}
     *
     * <p>Tickers with insufficient price history are skipped rather than aborting the
     * entire calibration — a single bad market feed should not kill the scenario run.
     */
    @Override
    public MarketData calibrate(LocalDate asOfDate, Map<String, double[]> historicalPrices) {
        return calibrateFromPrices(asOfDate, historicalPrices);
    }

    public MarketData calibrateFromPrices(LocalDate asOfDate,
                                          Map<String, double[]> historicalPrices) {

        List<String> tickers = new ArrayList<>(historicalPrices.keySet());
        int n = tickers.size();
        log.info("Calibrating market data for {} risk factor(s) as of {}", n, asOfDate);

        Map<String, double[]> returnsMap = new HashMap<>();
        List<String> skipped = new ArrayList<>();
        tickers.forEach(ticker -> {
            double[] prices = historicalPrices.get(ticker);
            if (prices == null || prices.length < 2) {
                log.warn("CALIBRATION | ticker={} skipped — requires at least 2 price observations (got {})",
                        ticker, prices == null ? 0 : prices.length);
                skipped.add(ticker);
                return;
            }
            try {
                returnsMap.put(ticker, calculateLogReturns(prices));
            } catch (Exception ex) {
                log.warn("CALIBRATION | ticker={} skipped — log-return computation failed: {}", ticker, ex.getMessage());
                skipped.add(ticker);
            }
        });

        // Remove skipped tickers from the working set
        tickers.removeAll(skipped);
        if (tickers.isEmpty()) {
            log.error("CALIBRATION | No valid tickers remain after resilience filtering — returning empty MarketData");
            return MarketData.builder()
                    .asOfDate(asOfDate)
                    .riskFactors(List.of())
                    .volatilities(Map.of())
                    .correlationMatrix(new double[0][0])
                    .covarianceMatrix(new double[0][0])
                    .historicalReturns(Map.of())
                    .build();
        }
        if (!skipped.isEmpty()) {
            log.warn("CALIBRATION | {}/{} tickers skipped: {}", skipped.size(), n, skipped);
        }

        Map<String, Double> volatilities = new HashMap<>();
        tickers.forEach(ticker -> {
            double vol = calculateAnnualizedVol(returnsMap.get(ticker));
            volatilities.put(ticker, vol);
            log.debug("Annualised vol for {}: {}", ticker, vol);
        });

        double[][] correlationMatrix = calculateCorrelationMatrix(tickers, returnsMap);

        int m = tickers.size();
        double[][] covarianceMatrix = new double[m][m];
        for (int i = 0; i < m; i++) {
            for (int j = 0; j < m; j++) {
                double volI = volatilities.get(tickers.get(i));
                double volJ = volatilities.get(tickers.get(j));
                covarianceMatrix[i][j] = correlationMatrix[i][j] * volI * volJ;
            }
        }

        log.info("Calibration complete — {}/{} risk factors, asOfDate={}", tickers.size(), n, asOfDate);
        return MarketData.builder()
                .asOfDate(asOfDate)
                .riskFactors(tickers)
                .volatilities(volatilities)
                .correlationMatrix(correlationMatrix)
                .covarianceMatrix(covarianceMatrix)
                .historicalReturns(returnsMap) // Cache historical log returns directly
                .build();
    }

    private double[] calculateLogReturns(double[] prices) {
        return IntStream.range(1, prices.length)
                .mapToDouble(i -> Math.log(prices[i]) - Math.log(prices[i - 1]))
                .toArray();
    }

    private double calculateAnnualizedVol(double[] returns) {
        double mean = Arrays.stream(returns).average().orElse(0.0);
        double variance = Arrays.stream(returns)
                .map(r -> Math.pow(r - mean, 2))
                .sum() / (returns.length - 1);
        return Math.sqrt(variance) * Math.sqrt(DAYS_IN_YEAR);
    }

    private double calculatePearsonCorrelation(double[] x, double[] y) {
        double meanX = Arrays.stream(x).average().orElse(0.0);
        double meanY = Arrays.stream(y).average().orElse(0.0);

        double numerator = 0.0;
        double denominatorX = 0.0;
        double denominatorY = 0.0;

        for (int i = 0; i < x.length; i++) {
            double diffX = x[i] - meanX;
            double diffY = y[i] - meanY;
            numerator += diffX * diffY;
            denominatorX += diffX * diffX;
            denominatorY += diffY * diffY;
        }

        if (denominatorX == 0.0 || denominatorY == 0.0) {
            log.warn("Constant return series detected; Pearson correlation set to 0.0");
            return 0.0;
        }

        return numerator / Math.sqrt(denominatorX * denominatorY);
    }

    private double[][] calculateCorrelationMatrix(List<String> tickers,
                                                  Map<String, double[]> returnsMap) {
        int n = tickers.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = calculatePearsonCorrelation(
                        returnsMap.get(tickers.get(i)),
                        returnsMap.get(tickers.get(j)));
            }
        }
        return matrix;
    }
}
