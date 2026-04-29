package com.kacemrisk.market.infrastructure.adapter.out.alphavantage.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

/**
 * Deserialisation DTO for the Alpha Vantage {@code FX_DAILY} endpoint response.
 *
 * <p>Used exclusively by the {@code FX} strategy. The JSON structure is similar to
 * {@link TimeSeriesDailyResponse} but uses a different time-series key and omits
 * the volume field.
 *
 * <p>Example response shape:
 * <pre>
 * {
 *   "Meta Data": { "2. From Symbol": "EUR", "3. To Symbol": "USD", ... },
 *   "Time Series FX (Daily)": {
 *     "2026-04-28": { "1. open": "1.0850", "4. close": "1.0880" }
 *   }
 * }
 * </pre>
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FxDailyResponse {

    @JsonProperty("Time Series FX (Daily)")
    private Map<String, FxBar> timeSeries;

    /**
     * Single bar (OHLC) for one FX trading day.
     * Only the closing mid-rate is extracted.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FxBar {

        @JsonProperty("4. close")
        private String close;
    }
}

