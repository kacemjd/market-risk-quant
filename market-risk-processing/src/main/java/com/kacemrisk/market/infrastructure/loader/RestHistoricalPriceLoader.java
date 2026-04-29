package com.kacemrisk.market.infrastructure.loader;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * {@link HistoricalPriceLoader} that fetches prices from the Alpha Vantage REST API.
 *
 * <p>Active when {@code input.source=rest} (default).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "input.source", havingValue = "rest", matchIfMissing = true)
@RequiredArgsConstructor
public class RestHistoricalPriceLoader implements HistoricalPriceLoader {

    private final FetchHistoricalPricesUseCase fetchHistoricalPricesUseCase;

    @Override
    public List<HistoricalPrice> load(Map<String, AssetClass> tickerAssetClass,
                                      LocalDate from, LocalDate to) {
        log.info("[REST] Fetching {} ticker(s) from Alpha Vantage | window=[{} → {}]",
                tickerAssetClass.size(), from, to);

        List<HistoricalPrice> result = fetchHistoricalPricesUseCase.fetch(
                FetchHistoricalPricesCommand.builder()
                        .tickers(tickerAssetClass)
                        .from(from)
                        .to(to)
                        .build());

        log.info("[REST] Received {} price records", result.size());
        return result;
    }
}
