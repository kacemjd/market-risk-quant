package domain.service.calibration;

import domain.model.MarketData;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.IntStream;

@Slf4j
public class MarketDataCalibrationService {

    private static final int DAYS_IN_YEAR = 252;

    public MarketData calibrateFromPrices(LocalDate asOfDate,
                                          Map<String, List<Double>> historicalPrices) {

        List<String> tickers = new ArrayList<>(historicalPrices.keySet());
        int n = tickers.size();
        log.info("Calibrating market data for {} risk factor(s) as of {}", n, asOfDate);

        Map<String, double[]> returnsMap = new HashMap<>();
        tickers.forEach(ticker -> {
            List<Double> prices = historicalPrices.get(ticker);
            if (prices == null || prices.size() < 2) {
                throw new IllegalArgumentException(
                        "Ticker " + ticker + " requires at least 2 price observations.");
            }
            returnsMap.put(ticker, calculateLogReturns(prices));
        });

        Map<String, Double> volatilities = new HashMap<>();
        tickers.forEach(ticker -> {
            double vol = calculateAnnualizedVol(returnsMap.get(ticker));
            volatilities.put(ticker, vol);
            log.debug("Annualised vol for {}: {}", ticker, vol);
        });

        double[][] correlationMatrix = calculateCorrelationMatrix(tickers, returnsMap);

        double[][] covarianceMatrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                double volI = volatilities.get(tickers.get(i));
                double volJ = volatilities.get(tickers.get(j));
                covarianceMatrix[i][j] = correlationMatrix[i][j] * volI * volJ;
            }
        }

        log.info("Calibration complete — {} risk factors, asOfDate={}", n, asOfDate);
        return MarketData.builder()
                .asOfDate(asOfDate)
                .riskFactors(tickers)
                .volatilities(volatilities)
                .correlationMatrix(correlationMatrix)
                .covarianceMatrix(covarianceMatrix)
                .build();
    }

    private double[] calculateLogReturns(List<Double> prices) {
        return IntStream.range(1, prices.size())
                .mapToDouble(i -> Math.log(prices.get(i) / prices.get(i - 1)))
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

        double numerator    = 0.0;
        double denominatorX = 0.0;
        double denominatorY = 0.0;

        for (int i = 0; i < x.length; i++) {
            double diffX = x[i] - meanX;
            double diffY = y[i] - meanY;
            numerator    += diffX * diffY;
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
