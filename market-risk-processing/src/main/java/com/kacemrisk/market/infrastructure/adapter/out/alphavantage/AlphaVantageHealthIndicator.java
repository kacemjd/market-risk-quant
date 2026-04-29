package com.kacemrisk.market.infrastructure.adapter.out.alphavantage;

import com.kacemrisk.market.infrastructure.config.AlphaVantageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * Spring Boot Actuator {@link HealthIndicator} for the Alpha Vantage REST API.
 *
 * <p>Issues a lightweight {@code GLOBAL_QUOTE} probe for the configured
 * {@code alphavantage.health-check-ticker} (default: {@code IBM}).
 * {@code GLOBAL_QUOTE} returns a single-record payload — it is the cheapest
 * non-trivial call available on the free tier, and IBM is always present.
 *
 * <h3>Status mapping</h3>
 * <table border="1">
 *   <tr><th>Condition</th><th>Status</th></tr>
 *   <tr><td>Valid quote returned</td><td>UP</td></tr>
 *   <tr><td>API returns {@code "Information"} (rate-limited / invalid key)</td><td>DOWN</td></tr>
 *   <tr><td>API returns {@code "Note"} (soft rate-limit)</td><td>DOWN</td></tr>
 *   <tr><td>Network error / timeout</td><td>DOWN</td></tr>
 * </table>
 *
 * <p>Exposed at {@code GET /actuator/health/alphaVantage} when Actuator is on the classpath.
 */
@Slf4j
@Component("alphaVantage")
@RequiredArgsConstructor
public class AlphaVantageHealthIndicator implements HealthIndicator {

    private static final String FUNCTION = "GLOBAL_QUOTE";
    private static final String QUOTE_KEY = "Global Quote";
    private static final String SYMBOL_KEY = "01. symbol";
    private static final String INFO_KEY = "Information";
    private static final String NOTE_KEY = "Note";

    private final WebClient alphaVantageWebClient;
    private final AlphaVantageProperties properties;

    @Override
    public Health health() {
        String ticker = properties.getHealthCheckTicker();
        log.debug("[AlphaVantageHealth] Probing GLOBAL_QUOTE for ticker={}", ticker);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> body = alphaVantageWebClient.get()
                    .uri(ub -> ub
                            .queryParam("function", FUNCTION)
                            .queryParam("symbol", ticker)
                            .queryParam("apikey", properties.getApiKey())
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block(Duration.ofSeconds(properties.getResponseTimeoutSeconds()));

            if (body == null) {
                return Health.down().withDetail("reason", "empty response").build();
            }

            // Alpha Vantage signals rate-limit / auth errors inside a 200 OK body
            if (body.containsKey(INFO_KEY)) {
                return Health.down()
                        .withDetail("reason", "api-key limited or invalid")
                        .withDetail(INFO_KEY, body.get(INFO_KEY))
                        .build();
            }
            if (body.containsKey(NOTE_KEY)) {
                return Health.down()
                        .withDetail("reason", "rate-limited (Note)")
                        .withDetail(NOTE_KEY, body.get(NOTE_KEY))
                        .build();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> quote = (Map<String, Object>) body.get(QUOTE_KEY);
            if (quote == null || quote.isEmpty()) {
                return Health.down()
                        .withDetail("reason", "quote object missing or empty")
                        .withDetail("ticker", ticker)
                        .build();
            }

            return Health.up()
                    .withDetail("ticker", ticker)
                    .withDetail("symbol", quote.getOrDefault(SYMBOL_KEY, ticker))
                    .build();

        } catch (Exception ex) {
            log.warn("[AlphaVantageHealth] Probe failed | ticker={} | error={}", ticker, ex.getMessage());
            return Health.down(ex)
                    .withDetail("ticker", ticker)
                    .build();
        }
    }
}

