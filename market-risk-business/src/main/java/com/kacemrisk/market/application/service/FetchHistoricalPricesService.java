package com.kacemrisk.market.application.service;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Application service — delegates bulk historical price fetching to the
 * {@link HistoricalPriceProvider} in a single {@link HistoricalPriceProvider#fetchAll} call.
 *
 * <p>Per-ticker error resilience is the adapter's responsibility:
 * {@code AlphaVantageHistoricalPriceAdapter} uses {@code Flux.flatMap(concurrency)}
 * with per-ticker {@code onErrorResume}; {@code CsvHistoricalPriceLoader} returns an
 * empty list on missing files. This service remains a thin, framework-free orchestrator.
 */
@Slf4j
@RequiredArgsConstructor
public class FetchHistoricalPricesService implements FetchHistoricalPricesUseCase {

    private final HistoricalPriceProvider provider;

    @Override
    public List<HistoricalPrice> fetch(FetchHistoricalPricesCommand command) {
        log.info("Bulk historical fetch | tickers={} | from={} | to={}",
                command.getTickers().keySet(), command.getFrom(), command.getTo());

        List<HistoricalPrice> result = provider.fetchAll(
                command.getTickers(), command.getFrom(), command.getTo());

        log.info("Bulk historical fetch completed | tickers={} | records={}",
                command.getTickers().keySet(), result.size());
        return result;
    }
}
