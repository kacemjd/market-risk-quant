package com.kacemrisk.market.application.port.out;

import com.kacemrisk.market.domain.model.MarketData;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Driven port — SPI that infrastructure adapters must implement to
 * provide raw market data to the domain.
 */
public interface MarketDataRepository {

    /**
     * Fetch the latest persisted {@link MarketData} for the given date.
     *
     * @param asOfDate the reference date
     * @return an {@link Optional} containing the market data if available
     */
    Optional<MarketData> findByDate(LocalDate asOfDate);

    /**
     * Persist calibrated market data so it can be reused downstream.
     *
     * @param marketData the calibrated market data to save
     */
    void save(MarketData marketData);
}

