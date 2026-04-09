package com.kacemrisk.market.infrastructure.adapter.in.rest;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VaRResultResponse {
    String correlationId;
    String portfolioId;
    String asOfDate;
    String method;
    double var;
    double expectedShortfall;
    double alpha;
    double meanPnL;
    double stdDevPnL;
    int    numScenarios;
}

