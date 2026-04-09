package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRMethod;
import lombok.Builder;
import lombok.Value;

/**
 * Command object carrying all inputs required by the Application layer
 * to execute a VaR calculation.
 */
@Value
@Builder
public class CalculateVaRCommand {
    Portfolio portfolio;
    MarketData marketData;

    VaRMethod method;
    double alpha;

    // Configuration parameters for specific strategies
    int numPaths;
    int historicalWindow;
    MaturityGrid timeGrid;
}

