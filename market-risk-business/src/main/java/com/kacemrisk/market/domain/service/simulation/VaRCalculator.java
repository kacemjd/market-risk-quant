package com.kacemrisk.market.domain.service.simulation;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;

public interface VaRCalculator {

    VaRResult calculate(Portfolio portfolio, MarketData marketData, double alpha);
}
