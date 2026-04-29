package com.kacemrisk.market.infrastructure.adapter.out.alphavantage.strategy;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import com.kacemrisk.market.infrastructure.adapter.out.alphavantage.dto.FxDailyResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@Slf4j
@Component
public class FxAlphaVantageStrategy implements AlphaVantageRequestStrategy {

    private static final int CURRENCY_CODE_LENGTH = 3;

    @Override
    public boolean supports(AssetClass assetClass) {
        return AssetClass.FX == assetClass;
    }

    @Override
    public Flux<HistoricalPrice> fetch(String ticker, LocalDate from, LocalDate to,
                                       WebClient client, String apiKey, String outputSize) {
        String fromCurrency = ticker.substring(0, CURRENCY_CODE_LENGTH);
        String toCurrency   = ticker.substring(CURRENCY_CODE_LENGTH);

        log.debug("AlphaVantage FX request | function=FX_DAILY | pair={} ({}/{})",
                ticker, fromCurrency, toCurrency);

        return client.get()
                .uri(ub -> ub
                        .queryParam("function", "FX_DAILY")
                        .queryParam("from_symbol", fromCurrency)
                        .queryParam("to_symbol", toCurrency)
                        .queryParam("outputsize", outputSize)
                        .queryParam("apikey", apiKey)
                        .build())
                .retrieve()
                .bodyToMono(FxDailyResponse.class)
                .flatMapMany(resp -> parseTimeSeries(ticker, resp, from, to));
    }

    private Flux<HistoricalPrice> parseTimeSeries(String ticker,
                                                   FxDailyResponse resp,
                                                   LocalDate from, LocalDate to) {
        if (resp.getTimeSeries() == null) {
            log.warn("Empty FX_DAILY response for ticker={}", ticker);
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

