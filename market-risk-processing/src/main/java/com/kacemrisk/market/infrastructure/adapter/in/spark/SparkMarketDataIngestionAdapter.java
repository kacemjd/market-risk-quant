package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.domain.model.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class SparkMarketDataIngestionAdapter {

    private final SparkSession              spark;
    private final MarketDataRepository      marketDataRepository;
    private final CalibrateMarketDataUseCase calibrateMarketData;

    public MarketData ingest(String csvPath, LocalDate asOfDate) {
        log.info("Ingesting from '{}' as of {}", csvPath, asOfDate);

        Map<String, List<Double>> tickerPrices = readPrices(
                spark.read().option("header", "true").csv(csvPath), asOfDate);

        log.info("Collected prices for {} ticker(s): {}", tickerPrices.size(), tickerPrices.keySet());

        MarketData marketData = calibrateMarketData.calibrate(asOfDate, tickerPrices);
        marketDataRepository.save(marketData);
        return marketData;
    }

    public MarketData ingestDirectory(String pricesDir, LocalDate asOfDate) {
        log.info("Ingesting all tickers from '{}' as of {}", pricesDir, asOfDate);

        Map<String, List<Double>> tickerPrices = readPrices(
                spark.read().option("header", "true").csv(pricesDir + "/*.csv"), asOfDate);

        log.info("Collected prices for {} ticker(s): {}", tickerPrices.size(), tickerPrices.keySet());

        MarketData marketData = calibrateMarketData.calibrate(asOfDate, tickerPrices);
        marketDataRepository.save(marketData);
        log.info("Market data saved for {} ticker(s) as of {}", tickerPrices.size(), asOfDate);
        return marketData;
    }

    private Map<String, List<Double>> readPrices(Dataset<Row> raw, LocalDate asOfDate) {
        Map<String, List<Double>> tickerPrices = new LinkedHashMap<>();
        raw.select(
                        upper(col("Ticker")).as("ticker"),
                        col("Date"),
                        col("Close").cast("double").as("close"))
                .filter(col("Date").leq(asOfDate.toString()))
                .filter(col("close").isNotNull())
                .sort("ticker", "Date")
                .toLocalIterator()
                .forEachRemaining(row -> tickerPrices
                        .computeIfAbsent(row.getString(0), k -> new ArrayList<>())
                        .add(row.getDouble(2)));
        return tickerPrices;
    }
}
