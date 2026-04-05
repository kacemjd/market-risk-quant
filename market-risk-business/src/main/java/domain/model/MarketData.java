package domain.model;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Value
@Builder
public class MarketData {

    LocalDate asOfDate;
    Map<String, Double> volatilities;
    double[][] correlationMatrix;
    double[][] covarianceMatrix;
    List<String> riskFactors;
    Map<String, double[]> historicalReturns;

    public double getVolFor(String ticker) {
        return volatilities.getOrDefault(ticker, 0.0);
    }
}
