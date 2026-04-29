package com.kacemrisk.market.infrastructure.adapter.out.alphavantage.strategy;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import com.kacemrisk.market.infrastructure.adapter.out.alphavantage.dto.TimeSeriesDailyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Slf4j
@Component
public class EquityAlphaVantageStrategy implements AlphaVantageRequestStrategy {

    @Override
    public boolean supports(AssetClass assetClass) {
        return AssetClass.EQD == assetClass;
    }

    @Override
    public Flux<HistoricalPrice> fetch(String ticker, LocalDate from, LocalDate to,
                                       WebClient client, String apiKey, String outputSize) {
        log.debug("AlphaVantage EQD request | function=TIME_SERIES_DAILY | ticker={}", ticker);

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
            log.warn("Empty TIME_SERIES_DAILY response for ticker={}", ticker);
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

