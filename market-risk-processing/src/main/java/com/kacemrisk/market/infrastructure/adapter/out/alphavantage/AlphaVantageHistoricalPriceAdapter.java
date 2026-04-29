package com.kacemrisk.market.infrastructure.adapter.out.alphavantage;

import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.domain.exception.HistoricalPriceFetchException;
import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import com.kacemrisk.market.infrastructure.adapter.out.alphavantage.strategy.AlphaVantageRequestStrategy;
import com.kacemrisk.market.infrastructure.config.AlphaVantageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

/**
 * Outbound adapter — implements {@link HistoricalPriceProvider} by delegating
 * to the appropriate {@link AlphaVantageRequestStrategy} based on the instrument's
 * {@link AssetClass}.
 *
 * <p>Reactor is an internal implementation detail of this adapter — it is never
 * exposed through the port contract.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlphaVantageHistoricalPriceAdapter implements HistoricalPriceProvider {

    private final List<AlphaVantageRequestStrategy> strategies;
    private final WebClient alphaVantageWebClient;
    private final AlphaVantageProperties properties;

    @Value("${alphavantage.fetch-timeout-seconds:60}")
    private int fetchTimeoutSeconds;

    @Override
    public List<HistoricalPrice> fetch(String ticker, AssetClass assetClass,
                                       LocalDate from, LocalDate to) {
        log.debug("Fetching | ticker={} | assetClass={} | from={} | to={}", ticker, assetClass, from, to);

        AlphaVantageRequestStrategy strategy = strategies.stream()
                .filter(s -> s.supports(assetClass))
                .findFirst()
                .orElseThrow(() -> new HistoricalPriceFetchException(ticker, assetClass,
                        "No AlphaVantageRequestStrategy registered for assetClass=" + assetClass));

        return strategy.fetch(ticker, from, to,
                        alphaVantageWebClient,
                        properties.getApiKey(),
                        properties.getOutputSize())
                .onErrorMap(ex -> !(ex instanceof HistoricalPriceFetchException),
                        ex -> new HistoricalPriceFetchException(ticker, assetClass,
                                "Strategy execution failed: " + ex.getMessage(), ex))
                .collectList()
                .block(Duration.ofSeconds(fetchTimeoutSeconds));
    }
}
