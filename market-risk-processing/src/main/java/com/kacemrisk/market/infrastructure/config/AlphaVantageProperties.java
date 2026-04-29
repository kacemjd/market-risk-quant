package com.kacemrisk.market.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration for the Alpha Vantage REST API.
 *
 * <p>Bound from the {@code alphavantage.*} properties block in {@code application.yml}.
 * Activate or override per Spring profile as needed.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "alphavantage")
public class AlphaVantageProperties {

    /**
     * API key injected from the {@code ALPHA_VANTAGE_API_KEY} environment variable.
     * Falls back to {@code "demo"} which is rate-limited to a handful of tickers.
     */
    private String apiKey = "demo";

    /** Base URL of the Alpha Vantage query endpoint. */
    private String baseUrl = "https://www.alphavantage.co/query";

    /**
     * Controls how many days of data are returned per request.
     * {@code "compact"} → last 100 trading days (default, free-tier friendly).
     * {@code "full"}    → up to 20 years of data.
     */
    private String outputSize = "compact";

    /**
     * Maximum number of concurrent HTTP requests issued to Alpha Vantage.
     * The free tier is capped at 25 requests/day; keep this low to avoid 429s.
     */
    private int fetchConcurrency = 5;
}

