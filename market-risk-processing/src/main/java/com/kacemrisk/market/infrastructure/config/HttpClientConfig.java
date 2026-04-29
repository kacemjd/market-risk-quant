package com.kacemrisk.market.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Infrastructure configuration for outbound HTTP clients.
 *
 * <p>Registers a pre-configured {@link WebClient} bean scoped to the Alpha Vantage
 * base URL. Strategies inject this client directly via constructor injection so
 * they never need to reproduce boilerplate connection setup.
 */
@Configuration
@EnableConfigurationProperties(AlphaVantageProperties.class)
public class HttpClientConfig {

    /**
     * WebClient pre-configured with the Alpha Vantage base URL and JSON accept header.
     * Query parameters (function, symbol, apikey …) are appended per request by each strategy.
     */
    @Bean
    public WebClient alphaVantageWebClient(AlphaVantageProperties properties) {
        return WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

