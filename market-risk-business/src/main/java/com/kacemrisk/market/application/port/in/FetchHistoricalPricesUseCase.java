package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.application.service.historical.FetchHistoricalPricesService;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import reactor.core.publisher.Flux;

/**
 * Driving port — entry point for triggering a bulk historical price fetch
 * across all positions in a portfolio.
 *
 * <p>Implementations live in the application service layer
 * ({@link FetchHistoricalPricesService}).
 */
public interface FetchHistoricalPricesUseCase {

    /**
     * Fetch historical closing prices for every position in the given portfolio,
     * applying a bounded concurrency cap so external API rate limits are respected.
     *
     * <p>Positions that cannot be fetched are <em>skipped</em> and an audit
     * log entry is emitted — the stream never terminates early due to a single
     * instrument failure.
     *
     * @param command portfolio + date-range inputs
     * @return merged {@link Flux} of all successfully retrieved {@link HistoricalPrice} records
     */
    Flux<HistoricalPrice> fetchForPortfolio(FetchHistoricalPricesCommand command);
}

