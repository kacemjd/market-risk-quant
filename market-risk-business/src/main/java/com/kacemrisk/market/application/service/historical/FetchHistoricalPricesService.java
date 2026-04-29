package com.kacemrisk.market.application.service.historical;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.domain.exception.HistoricalPriceFetchException;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

/**
 * Application service that orchestrates bulk historical price fetching across
 * an entire portfolio.
 *
 * <p>It fans out individual per-position requests to the {@link HistoricalPriceProvider}
 * driven port using a bounded concurrency ceiling so external API rate limits are
 * respected (e.g. 5 simultaneous requests for the Alpha Vantage free tier).
 *
 * <p>Each failed position is <em>audited</em> via a structured {@code WARN} log entry
 * and then skipped — the aggregate stream keeps flowing for all remaining positions.
 * This follows the same non-blocking, audit-first approach as the VaR pipeline's
 * error handling.
 */
@Slf4j
public class FetchHistoricalPricesService implements FetchHistoricalPricesUseCase {

    private final HistoricalPriceProvider provider;

    /**
     * Maximum number of concurrent in-flight requests to the market-data provider.
     * Configured by infrastructure at wire-up time (see {@code DomainConfig}).
     */
    private final int fetchConcurrency;

    public FetchHistoricalPricesService(HistoricalPriceProvider provider, int fetchConcurrency) {
        this.provider = provider;
        this.fetchConcurrency = fetchConcurrency;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@code Flux.flatMap} overload with a concurrency parameter so at most
     * {@code fetchConcurrency} HTTP calls are in flight simultaneously.
     */
    @Override
    public Flux<HistoricalPrice> fetchForPortfolio(FetchHistoricalPricesCommand command) {
        log.info("Bulk historical fetch started | portfolio={} | from={} | to={} | concurrency={}",
                command.getPortfolio().getId(),
                command.getFrom(),
                command.getTo(),
                fetchConcurrency);

        return Flux.fromIterable(command.getPortfolio().getPositions())
                .flatMap(pos ->
                        provider.fetch(pos.getTicker(), pos.getAssetClass(),
                                        command.getFrom(), command.getTo())
                                // Structured audit: HistoricalPriceFetchException carries ticker + assetClass
                                .onErrorResume(HistoricalPriceFetchException.class, ex -> {
                                    log.warn("AUDIT | fetch skipped | ticker={} | assetClass={} | reason={}",
                                            ex.getTicker(), ex.getAssetClass(), ex.getMessage());
                                    return Flux.empty();
                                })
                                // Catch-all for unexpected infrastructure errors
                                .onErrorResume(ex -> {
                                    log.warn("AUDIT | fetch skipped | ticker={} | unexpected error | reason={}",
                                            pos.getTicker(), ex.getMessage());
                                    return Flux.empty();
                                }),
                        fetchConcurrency)
                .doOnComplete(() ->
                        log.info("Bulk historical fetch completed | portfolio={}", command.getPortfolio().getId()));
    }
}

