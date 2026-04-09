package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;

public interface VaRPipeline {

    VaRResult execute(Portfolio portfolio, MarketData marketData, ScenarioNotification notification);
}

