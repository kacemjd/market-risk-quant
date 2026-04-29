package com.kacemrisk.market.infrastructure.loader;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Strategy port — abstracts the origin of historical price data.
 *
 * <p>The active implementation is selected by {@code input.source} in {@code application.yml}:
 * <ul>
 *   <li>{@code rest} → {@link RestHistoricalPriceLoader} (Alpha Vantage API — default)</li>
 *   <li>{@code csv}  → {@link CsvHistoricalPriceLoader}  (local CSV files)</li>
 * </ul>
 *
 * <p>{@link com.kacemrisk.market.infrastructure.runner.VaRRunner} depends only on this interface
 * and never needs to change when a new source is added.
 */
public interface HistoricalPriceLoader {

    /**
     * Loads closing prices for every entry in {@code tickerAssetClass} over [{@code from}, {@code to}].
     *
     * <p>Tickers that fail to load are silently skipped — the result may contain fewer tickers
     * than requested. Callers should log a warning for missing tickers.
     *
     * @param tickerAssetClass tickers to fetch, keyed by their asset class
     * @param from             start of the historical window (inclusive)
     * @param to               end of the historical window (inclusive, typically today)
     * @return flat list of {@link HistoricalPrice} records; never {@code null}
     */
    List<HistoricalPrice> load(Map<String, AssetClass> tickerAssetClass,
                               LocalDate from, LocalDate to);
}

