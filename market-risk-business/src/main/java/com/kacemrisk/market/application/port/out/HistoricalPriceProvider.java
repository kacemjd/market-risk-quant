package com.kacemrisk.market.application.port.out;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface HistoricalPriceProvider {

    /**
     * Fetch historical closing prices for a single instrument within the specified date window.
     *
     * @param ticker instrument identifier (stock symbol, FX pair, ETF symbol …)
     * @param assetClass asset class used to select the correct market-data endpoint
     * @param from start of the date range (inclusive)
     * @param to end of the date range (inclusive)
     * @return list of {@link HistoricalPrice} records, potentially empty
     */
    List<HistoricalPrice> fetch(String ticker, AssetClass assetClass, LocalDate from, LocalDate to);

    /**
     * Fetch historical closing prices for <em>all</em> tickers in one call.
     *
     * <p>The default implementation calls {@link #fetch} sequentially for each ticker. Adapters
     * that support concurrency (e.g. {@code AlphaVantageHistoricalPriceAdapter})
     * <strong>must</strong> override this method with a concurrent implementation so that
     * per-ticker API calls are parallelised up to their configured concurrency limit.
     *
     * <p>Tickers that fail are silently skipped by the overriding adapter — callers receive a
     * partial result rather than an exception for individual ticker failures.
     *
     * @param tickers map of ticker → {@link AssetClass} for every instrument to fetch
     * @param from start of the date range (inclusive)
     * @param to end of the date range (inclusive)
     * @return flat list of {@link HistoricalPrice} records across all tickers
     */
    default List<HistoricalPrice> fetchAll(
            Map<String, AssetClass> tickers, LocalDate from, LocalDate to) {
        List<HistoricalPrice> result = new ArrayList<>();
        tickers.forEach((ticker, assetClass) -> result.addAll(fetch(ticker, assetClass, from, to)));
        return result;
    }
}
