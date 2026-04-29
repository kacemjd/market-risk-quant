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
 * Alpha Vantage strategy for <strong>Interest Rate Derivative (IRD)</strong> instruments.
 *
 * <p>The portfolio represents IRD exposure through bond ETF proxies
 * (e.g. TLT → 20+ Year Treasury, IEF → 7–10 Year Treasury, SHY → 1–3 Year Treasury,
 * LQD → Investment-Grade Corporate Bonds, HYG → High-Yield Bonds).
 * These are exchange-listed and therefore use the same {@code TIME_SERIES_DAILY} endpoint.
 *
 * <p><strong>Extension note:</strong> To onboard actual interest-rate instruments
 * (swap rates, government bond yields), replace this strategy to call Alpha Vantage's
 * {@code FEDERAL_FUNDS_RATE}, {@code TREASURY_YIELD}, or similar functions without
 * modifying any other strategy or the adapter.
 *
 * <p>Alpha Vantage endpoint (current):
 * {@code GET /query?function=TIME_SERIES_DAILY&symbol=TLT&outputsize=compact&apikey=…}
 */
@Slf4j
@Component
public class IrdAlphaVantageStrategy implements AlphaVantageRequestStrategy {

    @Override
    public boolean supports(AssetClass assetClass) {
        return AssetClass.IRD == assetClass;
    }

    @Override
    public Flux<HistoricalPrice> fetch(String ticker, LocalDate from, LocalDate to,
                                       WebClient client, String apiKey, String outputSize) {
        log.debug("AlphaVantage IRD request | function=TIME_SERIES_DAILY (ETF proxy) | ticker={}", ticker);

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
            log.warn("Empty TIME_SERIES_DAILY response for IRD ticker={}", ticker);
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

