package com.kacemrisk.market.application.port.out;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;

import java.time.LocalDate;
import java.util.List;

/**
 * Driven port — SPI that infrastructure adapters must implement to supply
 * historical closing prices for a single instrument.
 *
 * <p>Implementations are asset-class–aware so they can dispatch to the correct
 * endpoint (e.g. {@code TIME_SERIES_DAILY} for equities, {@code FX_DAILY} for
 * currency pairs).
 *
 * <p>On unrecoverable errors the implementation throws
 * {@link com.kacemrisk.market.domain.exception.HistoricalPriceFetchException}
 * so callers can apply consistent audit handling.
 */
public interface HistoricalPriceProvider {

    /**
     * Fetch historical closing prices for the given instrument within the
     * specified date window.
     *
     * @param ticker     instrument identifier (stock symbol, FX pair, ETF symbol …)
     * @param assetClass asset class used to select the correct market-data endpoint
     * @param from       start of the date range (inclusive)
     * @param to         end of the date range (inclusive)
     * @return list of {@link HistoricalPrice} records, potentially empty
     */
    List<HistoricalPrice> fetch(String ticker, AssetClass assetClass,
                                LocalDate from, LocalDate to);
}
