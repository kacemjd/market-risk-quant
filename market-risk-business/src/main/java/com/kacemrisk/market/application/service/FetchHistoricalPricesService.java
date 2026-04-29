package com.kacemrisk.market.application.service;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.domain.exception.HistoricalPriceFetchException;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * Application service — orchestrates bulk historical price fetching across
 * all tickers in the command, applying per-ticker error resilience.
 *
 * <p>Framework-free: no Spring, no Reactor. Concurrency is delegated to the
 * {@link HistoricalPriceProvider} implementation (infrastructure concern).
 */
@Slf4j
@RequiredArgsConstructor
public class FetchHistoricalPricesService implements FetchHistoricalPricesUseCase {

    private final HistoricalPriceProvider provider;

    @Override
    public List<HistoricalPrice> fetch(FetchHistoricalPricesCommand command) {
        log.info("Bulk historical fetch started | tickers={} | from={} | to={}",
                command.getTickers().keySet(), command.getFrom(), command.getTo());

        List<HistoricalPrice> result = new ArrayList<>();

        command.getTickers().forEach((ticker, assetClass) -> {
            try {
                List<HistoricalPrice> prices = provider.fetch(
                        ticker, assetClass, command.getFrom(), command.getTo());
                result.addAll(prices);
            } catch (HistoricalPriceFetchException ex) {
                log.warn("AUDIT | fetch skipped | ticker={} | assetClass={} | reason={}",
                        ex.getTicker(), ex.getAssetClass(), ex.getMessage());
            } catch (Exception ex) {
                log.warn("AUDIT | fetch skipped | ticker={} | unexpected error | reason={}",
                        ticker, ex.getMessage());
            }
        });

        log.info("Bulk historical fetch completed | tickers={} | records={}",
                command.getTickers().keySet(), result.size());
        return result;
    }
}
