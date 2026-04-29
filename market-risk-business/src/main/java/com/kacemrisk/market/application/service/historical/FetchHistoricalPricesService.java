package com.kacemrisk.market.application.service.historical;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.domain.exception.HistoricalPriceFetchException;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

@Slf4j
public class FetchHistoricalPricesService implements FetchHistoricalPricesUseCase {

    private final HistoricalPriceProvider provider;

    private final int fetchConcurrency;

    public FetchHistoricalPricesService(HistoricalPriceProvider provider, int fetchConcurrency) {
        this.provider = provider;
        this.fetchConcurrency = fetchConcurrency;
    }

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

