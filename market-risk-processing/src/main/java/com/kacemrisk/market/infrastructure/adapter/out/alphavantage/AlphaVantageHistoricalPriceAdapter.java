package com.kacemrisk.market.infrastructure.adapter.out.alphavantage;

import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.domain.exception.HistoricalPriceFetchException;
import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import com.kacemrisk.market.infrastructure.adapter.out.alphavantage.strategy.AlphaVantageRequestStrategy;
import com.kacemrisk.market.infrastructure.config.AlphaVantageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.util.List;

/**
 * Outbound adapter — implements {@link HistoricalPriceProvider} by delegating
 * to the appropriate {@link AlphaVantageRequestStrategy} based on the instrument's
 * {@link AssetClass}.
 *
 * <p>Spring auto-collects all {@code @Component} strategy implementations into the
 * injected {@code List<AlphaVantageRequestStrategy>}, so adding a new asset class
 * only requires creating a new strategy bean — this class never changes.
 *
 * <p>On failure the error is wrapped as a {@link HistoricalPriceFetchException}
 * (carrying {@code ticker} + {@code assetClass} for structured audit logging)
 * so the upstream {@code FetchHistoricalPricesService} can apply consistent
 * {@code onErrorResume} handling across the bulk fetch stream.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlphaVantageHistoricalPriceAdapter implements HistoricalPriceProvider {

    private final List<AlphaVantageRequestStrategy> strategies;
    private final WebClient alphaVantageWebClient;
    private final AlphaVantageProperties properties;

    @Override
    public Flux<HistoricalPrice> fetch(String ticker, AssetClass assetClass,
                                       LocalDate from, LocalDate to) {
        return strategies.stream()
                .filter(s -> s.supports(assetClass))
                .findFirst()
                .map(strategy -> {
                    log.debug("Dispatching to {} | ticker={} | from={} | to={}",
                            strategy.getClass().getSimpleName(), ticker, from, to);
                    return strategy.fetch(ticker, from, to,
                            alphaVantageWebClient,
                            properties.getApiKey(),
                            properties.getOutputSize())
                            // Wrap any strategy-level error so callers get a HistoricalPriceFetchException
                            // with full ticker + assetClass context for audit purposes.
                            .onErrorMap(ex -> !(ex instanceof HistoricalPriceFetchException),
                                    ex -> new HistoricalPriceFetchException(ticker, assetClass,
                                            "Strategy execution failed: " + ex.getMessage(), ex));
                })
                .orElseGet(() -> Flux.error(
                        new HistoricalPriceFetchException(ticker, assetClass,
                                "No AlphaVantageRequestStrategy registered for assetClass=" + assetClass)));
    }
}

