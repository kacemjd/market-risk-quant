package com.kacemrisk.market.infrastructure.adapter.in.rest;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class VaRHistoryResponse {
    String              portfolioId;
    String              method;
    int                 totalPoints;
    // Summary stats
    double              latestVar;
    double              minVar;
    double              maxVar;
    double              avgVar;
    // Chronological time series (oldest → newest)
    List<VaRDataPoint>  series;

    @Value
    @Builder
    public static class VaRDataPoint {
        String asOfDate;
        double var;
        double expectedShortfall;
        double alpha;
    }
}

