package com.kacemrisk.market.infrastructure.loader;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Fetches and serialises the market price series for a given ticker universe.
 *
 * <p>Returns a {@code Map<String, double[]>} keyed by ticker where each array holds
 * closing prices sorted <b>oldest → newest</b> (primitive, zero boxing):
 * <ul>
 *   <li>{@code array[i]}            — historical close on trading day {@code i}</li>
 *   <li>{@code array[array.length-1]} — spot price (latest close as of {@code to})</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MarketPriceService {

    private final HistoricalPriceLoader historicalPriceLoader;

    /**
     * Loads and serialises closing prices for every ticker in the provided map.
     *
     * @param tickerAssetClass tickers to fetch, keyed by their {@link AssetClass}
     * @param from             start of the calendar window (inclusive)
     * @param to               end of the calendar window (inclusive — typically the run's asOfDate)
     * @return map of ticker → {@code double[]} sorted oldest → newest;
     * empty map if no prices could be loaded
     */
    public Map<String, double[]> loadPrices(Map<String, AssetClass> tickerAssetClass,
                                            LocalDate from, LocalDate to) {
        log.info("[MarketPriceService] Fetching prices for {} ticker(s), window [{} → {}]",
                tickerAssetClass.size(), from, to);

        List<HistoricalPrice> raw = historicalPriceLoader.load(tickerAssetClass, from, to);
        if (raw.isEmpty()) {
            log.error("[MarketPriceService] No prices returned for window [{} → {}]", from, to);
            return Collections.emptyMap();
        }

        Map<String, double[]> result = raw.stream()
                .collect(Collectors.groupingBy(
                        HistoricalPrice::getTicker,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(HistoricalPrice::getDate))
                                        .mapToDouble(HistoricalPrice::getClosePrice)
                                        .toArray())));

        log.info("[MarketPriceService] Serialised {} ticker(s), {} total price records",
                result.size(), raw.size());
        return result;
    }
}

