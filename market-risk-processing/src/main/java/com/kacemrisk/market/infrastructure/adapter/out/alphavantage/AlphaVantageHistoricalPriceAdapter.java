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
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Outbound adapter — implements {@link HistoricalPriceProvider} by delegating
 * to the appropriate {@link AlphaVantageRequestStrategy} based on the instrument's
 * {@link AssetClass}.
 *
 * <p>Reactor is an internal implementation detail of this adapter — it is never
 * exposed through the port contract.
 *
 * <p>{@link #fetchAll} overrides the default sequential implementation with a
 * concurrent {@code Flux.flatMap(concurrency)} so that all tickers are fetched in
 * parallel up to {@code alphavantage.fetch-concurrency} simultaneous requests.
 * Individual ticker failures are logged and skipped rather than aborting the batch.
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

    /**
     * Per-ticker fetch — delegates to the asset-class–specific strategy.
     * Used as fallback by the default {@code fetchAll} implementation and
     * directly for single-instrument lookups.
     */
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

    /**
     * Bulk fetch — issues all ticker requests concurrently, bounded by
     * {@code alphavantage.fetch-concurrency}. Tickers that fail are logged at WARN
     * level and excluded from the result rather than aborting the batch.
     *
     * <p>This replaces the removed sequential {@code forEach} that was previously
     * in {@code FetchHistoricalPricesService} (Phase 2, P2.6).
     */
    @Override
    public List<HistoricalPrice> fetchAll(Map<String, AssetClass> tickers,
                                          LocalDate from, LocalDate to) {
        log.info("[AlphaVantage] Bulk fetch | {} ticker(s) | window=[{} → {}] | concurrency={}",
                tickers.size(), from, to, properties.getFetchConcurrency());

        return Flux.fromIterable(tickers.entrySet())
                .flatMap(entry -> {
                    String ticker = entry.getKey();
                    AssetClass assetClass = entry.getValue();
                    return strategies.stream()
                            .filter(s -> s.supports(assetClass))
                            .findFirst()
                            .map(strategy -> strategy.fetch(ticker, from, to,
                                            alphaVantageWebClient,
                                            properties.getApiKey(),
                                            properties.getOutputSize())
                                    .onErrorResume(ex -> {
                                        log.warn("AUDIT | fetch skipped | ticker={} | assetClass={} | reason={}",
                                                ticker, assetClass, ex.getMessage());
                                        return Flux.empty();
                                    }))
                            .orElseGet(() -> {
                                log.warn("AUDIT | fetch skipped | ticker={} | reason=no strategy for assetClass={}",
                                        ticker, assetClass);
                                return Flux.empty();
                            });
                }, properties.getFetchConcurrency())
                .collectList()
                .block(Duration.ofSeconds(fetchTimeoutSeconds));
    }
}
