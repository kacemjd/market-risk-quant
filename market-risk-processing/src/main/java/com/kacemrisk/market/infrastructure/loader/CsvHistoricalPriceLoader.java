package com.kacemrisk.market.infrastructure.loader;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link HistoricalPriceLoader} implementation that reads prices from local CSV files.
 *
 * <p>Active when {@code input.source=csv}. Used by the {@code local} profile so developers
 * can work offline without consuming Alpha Vantage API quota.
 *
 * <h3>Expected layout (classpath)</h3>
 * <pre>
 *   {input.csv.prices-path}/
 *       AAPL.csv
 *       MSFT.csv
 *       ...
 * </pre>
 *
 * <h3>CSV format</h3>
 * <pre>Ticker,Date,Open,High,Low,Close,Volume,OpenInt</pre>
 * Column names are detected case-insensitively; only {@code Date} and {@code Close} are required.
 * Rows outside [{@code from}, {@code to}] are silently skipped.
 *
 * <p>To seed the local prices directory, copy the test fixtures from
 * {@code src/test/resources/market-data/prices/} into
 * {@code src/main/resources/data/prices/}.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "input.source", havingValue = "csv")
public class CsvHistoricalPriceLoader implements HistoricalPriceLoader {

    @Value("${input.csv.prices-path:data/prices}")
    private String pricesPath;

    @Override
    public List<HistoricalPrice> load(Map<String, AssetClass> tickerAssetClass,
                                      LocalDate from, LocalDate to) {
        log.info("[CSV] Loading {} ticker(s) from classpath:{} | window=[{} → {}]",
                tickerAssetClass.size(), pricesPath, from, to);

        List<HistoricalPrice> result = new ArrayList<>();
        int loaded = 0;
        int missing = 0;
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

        for (String ticker : tickerAssetClass.keySet()) {
            String pattern = "classpath:" + pricesPath + "/" + ticker + ".csv";
            try {
                Resource[] resources = resolver.getResources(pattern);
                if (resources.length == 0) {
                    log.warn("[CSV] No file found for ticker '{}' at {}", ticker, pattern);
                    missing++;
                    continue;
                }
                List<HistoricalPrice> prices = parseCsv(resources[0], ticker, from, to);
                result.addAll(prices);
                log.debug("[CSV] ticker={} → {} price record(s)", ticker, prices.size());
                loaded++;
            } catch (Exception e) {
                log.warn("[CSV] Failed to load ticker '{}': {}", ticker, e.getMessage());
                missing++;
            }
        }

        log.info("[CSV] Loaded {} records | {}/{} tickers ok | {} missing",
                result.size(), loaded, tickerAssetClass.size(), missing);
        return result;
    }

    private List<HistoricalPrice> parseCsv(Resource resource, String ticker,
                                           LocalDate from, LocalDate to) throws Exception {
        List<HistoricalPrice> prices = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream()))) {

            String header = reader.readLine();
            if (header == null) return prices;

            String[] cols = header.split(",");
            int dateIdx = findColumn(cols, "Date");
            int closeIdx = findColumn(cols, "Close");

            if (dateIdx < 0 || closeIdx < 0) {
                log.warn("[CSV] ticker='{}' — CSV header '{}' missing Date or Close column", ticker, header);
                return prices;
            }

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length <= Math.max(dateIdx, closeIdx)) continue;
                try {
                    LocalDate date = LocalDate.parse(parts[dateIdx].trim());
                    double close = Double.parseDouble(parts[closeIdx].trim());
                    if (!date.isBefore(from) && !date.isAfter(to)) {
                        prices.add(HistoricalPrice.builder()
                                .ticker(ticker)
                                .date(date)
                                .closePrice(close)
                                .build());
                    }
                } catch (Exception ex) {
                    // Skip malformed rows silently
                }
            }
        }
        return prices;
    }

    /**
     * Case-insensitive column index lookup.
     */
    private int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase(name)) return i;
        }
        return -1;
    }
}

