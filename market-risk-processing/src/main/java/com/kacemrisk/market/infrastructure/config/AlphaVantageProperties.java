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

    /**
     * TCP connect timeout per request (seconds).
     * Surfaces unreachable hosts quickly rather than waiting for OS-level timeout.
     */
    private int connectTimeoutSeconds = 10;

    /**
     * Maximum time to wait for a complete HTTP response for a single ticker (seconds).
     * Applied at the Netty layer — fires before the batch-level {@link #fetchTimeoutSeconds}.
     */
    private int responseTimeoutSeconds = 30;

    /**
     * Maximum wall-clock time for a full {@code fetchAll} batch (seconds).
     * Acts as the outer safety net after per-request timeouts have already fired.
     */
    private int fetchTimeoutSeconds = 120;

    /**
     * Ticker used by {@code AlphaVantageHealthIndicator} for the lightweight probe call.
     * IBM is Alpha Vantage's canonical demo symbol and is always available on the free tier.
     */
    private String healthCheckTicker = "IBM";
}

