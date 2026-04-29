package com.kacemrisk.market.infrastructure.adapter.out.alphavantage.strategy;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * Strategy interface for Alpha Vantage request dispatching.
 *
 * <p>Each implementation is responsible for:
 * <ol>
 *   <li>Declaring which {@link AssetClass} it handles ({@link #supports}).</li>
 *   <li>Building the correct Alpha Vantage query URL for that asset class.</li>
 *   <li>Parsing the endpoint-specific JSON response into {@link HistoricalPrice} domain objects.</li>
 * </ol>
 *
 * <p>All implementations are registered as Spring {@code @Component}s so that
 * {@link com.kacemrisk.market.infrastructure.adapter.out.alphavantage.AlphaVantageHistoricalPriceAdapter}
 * can auto-collect them via {@code List<AlphaVantageRequestStrategy>} injection.
 */
public interface AlphaVantageRequestStrategy {

    /**
     * Returns {@code true} if this strategy can handle the given asset class.
     *
     * @param assetClass the asset class to check
     * @return {@code true} iff this strategy covers {@code assetClass}
     */
    boolean supports(AssetClass assetClass);

    /**
     * Execute the HTTP request and stream parsed {@link HistoricalPrice} records.
     *
     * @param ticker     instrument identifier (stock symbol, FX pair, ETF …)
     * @param from       start of the requested date window (inclusive)
     * @param to         end of the requested date window (inclusive)
     * @param client     pre-configured {@link WebClient} for the Alpha Vantage base URL
     * @param apiKey     Alpha Vantage API key
     * @param outputSize {@code "compact"} (100 days) or {@code "full"} (20 years)
     * @return {@link Flux} of matching {@link HistoricalPrice} records
     */
    Flux<HistoricalPrice> fetch(String ticker, LocalDate from, LocalDate to,
                                WebClient client, String apiKey, String outputSize);
}

