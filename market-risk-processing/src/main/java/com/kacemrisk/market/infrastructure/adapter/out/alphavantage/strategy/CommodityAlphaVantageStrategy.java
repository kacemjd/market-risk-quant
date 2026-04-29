package com.kacemrisk.market.infrastructure.adapter.out.alphavantage.strategy;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import com.kacemrisk.market.infrastructure.adapter.out.alphavantage.dto.TimeSeriesDailyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

/**
 * Alpha Vantage strategy for <strong>Commodity (CTY)</strong> instruments.
 *
 * <p>The portfolio uses <em>ETF proxies</em> for commodity exposure
 * (e.g. GLD → Gold, SLV → Silver, USO → WTI Crude Oil, CORN → Corn futures).
 * These are exchange-listed equity instruments and therefore share the same
 * {@code TIME_SERIES_DAILY} endpoint as the equity strategy.
 *
 * <p><strong>Extension note:</strong> To switch from ETF proxies to direct commodity
 * futures data, replace this implementation with calls to Alpha Vantage's dedicated
 * commodity functions ({@code CRUDE_OIL_PRICES}, {@code NATURAL_GAS}, {@code COPPER} …)
 * and introduce a corresponding DTO without breaking any other strategy or the adapter.
 *
 * <p>Alpha Vantage endpoint (current):
 * {@code GET /query?function=TIME_SERIES_DAILY&symbol=GLD&outputsize=compact&apikey=…}
 */
@Slf4j
@Component
public class CommodityAlphaVantageStrategy implements AlphaVantageRequestStrategy {

    @Override
    public boolean supports(AssetClass assetClass) {
        return AssetClass.CTY == assetClass;
    }

    @Override
    public Flux<HistoricalPrice> fetch(String ticker, LocalDate from, LocalDate to,
                                       WebClient client, String apiKey, String outputSize) {
        log.debug("AlphaVantage CTY request | function=TIME_SERIES_DAILY (ETF proxy) | ticker={}", ticker);

        return client.get()
                .uri(ub -> ub
                        .queryParam("function", "TIME_SERIES_DAILY")
                        .queryParam("symbol", ticker)
                        .queryParam("outputsize", outputSize)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(TimeSeriesDailyResponse.class)
                .flatMapMany(resp -> parseTimeSeries(ticker, resp, from, to));
    }

    private Flux<HistoricalPrice> parseTimeSeries(String ticker,
                                                   TimeSeriesDailyResponse resp,
                                                   LocalDate from, LocalDate to) {
        if (resp.getTimeSeries() == null) {
            log.warn("Empty TIME_SERIES_DAILY response for CTY ticker={}", ticker);
            return Flux.empty();
        }
        return Flux.fromIterable(resp.getTimeSeries().entrySet())
                .map(entry -> HistoricalPrice.builder()
                        .ticker(ticker)
                        .date(LocalDate.parse(entry.getKey()))
                        .closePrice(Double.parseDouble(entry.getValue().getClose()))
                        .build())
                .filter(hp -> !hp.getDate().isBefore(from) && !hp.getDate().isAfter(to));
    }
}

