package com.kacemrisk.market.infrastructure.adapter.out.alphavantage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Deserialisation DTO for the Alpha Vantage {@code TIME_SERIES_DAILY} endpoint response.
 *
 * <p>Used by {@code EQD}, {@code CTY} (ETF proxies), and {@code IRD} (ETF proxies) strategies.
 * Jackson maps the oddly-named JSON keys via {@link JsonProperty}.
 *
 * <p>Example response shape:
 * <pre>
 * {
 *   "Meta Data": { "2. Symbol": "AAPL", ... },
 *   "Time Series (Daily)": {
 *     "2026-04-28": { "1. open": "195.00", "4. close": "196.50", ... }
 *   }
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeSeriesDailyResponse {

    @JsonProperty("Time Series (Daily)")
    private Map<String, DailyBar> timeSeries;

    /**
     * Single bar (OHLCV) for one trading day.
     * Only the closing price is extracted for VaR / calibration purposes.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyBar {

        @JsonProperty("4. close")
        private String close;
    }
}

