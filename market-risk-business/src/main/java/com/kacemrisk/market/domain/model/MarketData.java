package com.kacemrisk.market.domain.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MarketData implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

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
