package com.kacemrisk.market.infrastructure.loader;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.domain.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * {@link HistoricalPriceLoader} implementation that fetches prices from the
 * Alpha Vantage REST API via {@link FetchHistoricalPricesUseCase}.
 *
 * <p>Active when {@code input.source=rest} (default — {@code matchIfMissing=true}).
 * Used by the {@code docker} profile and any environment where a live API key is available.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "input.source", havingValue = "rest", matchIfMissing = true)
@RequiredArgsConstructor
public class RestHistoricalPriceLoader implements HistoricalPriceLoader {

    private final FetchHistoricalPricesUseCase fetchHistoricalPricesUseCase;

    @Override
    public List<HistoricalPrice> load(Map<String, AssetClass> tickerAssetClass,
                                      LocalDate from, LocalDate to) {
        log.info("[REST] Fetching {} ticker(s) from Alpha Vantage | window=[{} → {}]",
                tickerAssetClass.size(), from, to);

        // Build a minimal stub portfolio — the use case only reads ticker + assetClass
        Portfolio stub = Portfolio.builder()
                .id("LOADER_STUB")
                .positions(tickerAssetClass.entrySet().stream()
                        .map(e -> Position.builder()
                                .ticker(e.getKey())
                                .assetClass(e.getValue())
                                .quantity(1.0)
                                .spotPrice(0.0)
                                .delta(1.0)
                                .gamma(0.0)
                                .maturityInYears(0.0)
                                .build())
                        .collect(Collectors.toList()))
                .build();

        List<HistoricalPrice> prices = fetchHistoricalPricesUseCase
                .fetchForPortfolio(FetchHistoricalPricesCommand.builder()
                        .portfolio(stub)
                        .from(from)
                        .to(to)
                        .build())
                .collectList()
                .block();

        int count = prices == null ? 0 : prices.size();
        log.info("[REST] Received {} price records", count);
        return prices == null ? List.of() : prices;
    }
}

