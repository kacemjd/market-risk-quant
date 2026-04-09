package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.MarketData;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface CalibrateMarketDataUseCase {

    MarketData calibrate(LocalDate asOfDate, Map<String, List<Double>> historicalPrices);
}

