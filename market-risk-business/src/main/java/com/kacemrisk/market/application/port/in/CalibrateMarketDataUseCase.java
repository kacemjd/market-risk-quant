package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.MarketData;
import java.time.LocalDate;
import java.util.Map;

public interface CalibrateMarketDataUseCase {

    /**
     * Calibrates market data (volatilities, covariance) from primitive price arrays.
     *
     * @param asOfDate the valuation date
     * @param historicalPrices map of ticker → close prices sorted oldest → newest
     */
    MarketData calibrate(LocalDate asOfDate, Map<String, double[]> historicalPrices);
}
