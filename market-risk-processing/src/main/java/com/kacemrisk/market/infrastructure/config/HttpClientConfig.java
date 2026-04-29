package com.kacemrisk.market.infrastructure.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Infrastructure configuration for outbound HTTP clients.
 *
 * <p>Registers a pre-configured {@link WebClient} bean scoped to the Alpha Vantage
 * base URL. Three layers of timeout are applied so that a stalled or unreachable
 * host fails fast well before the batch-level {@code block()} deadline:
 * <ol>
 *   <li><b>connectTimeout</b> — TCP handshake must complete within this window</li>
 *   <li><b>responseTimeout</b> — full HTTP response for a single ticker must arrive within this window</li>
 *   <li><b>fetchTimeoutSeconds</b> (in the adapter) — outer safety net for the whole batch</li>
 * </ol>
 */
@Configuration
@EnableConfigurationProperties(AlphaVantageProperties.class)
public class HttpClientConfig {

    /**
     * WebClient pre-configured with the Alpha Vantage base URL, JSON accept header,
     * and layered Netty-level timeouts read from {@link AlphaVantageProperties}.
     */
    @Bean
    public WebClient alphaVantageWebClient(AlphaVantageProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) Duration.ofSeconds(properties.getConnectTimeoutSeconds()).toMillis())
                .responseTimeout(Duration.ofSeconds(properties.getResponseTimeoutSeconds()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(
                                properties.getResponseTimeoutSeconds(), TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(
                                properties.getResponseTimeoutSeconds(), TimeUnit.SECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}

