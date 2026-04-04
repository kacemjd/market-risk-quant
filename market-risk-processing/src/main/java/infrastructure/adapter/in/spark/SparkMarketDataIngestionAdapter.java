package infrastructure.adapter.in.spark;

import application.port.out.MarketDataRepository;
import domain.model.MarketData;
import domain.service.calibration.MarketDataCalibrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.apache.spark.sql.functions.*;

/**
 * Inbound adapter — reads raw market data from a Spark data source
 * and delegates calibration to the domain use case.
 *
 * This is the entry point for batch/stream ingestion in the hexagonal architecture.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SparkMarketDataIngestionAdapter {

    private final SparkSession spark;
    private final MarketDataRepository marketDataRepository;
    private final MarketDataCalibrationService calibrationService;

    /**
     * Reads a single-ticker price CSV, calibrates market data up to {@code asOfDate},
     * persists the result and returns it.
     *
     * Expected CSV schema: Date, Open, High, Low, Close, Volume, OpenInt
     *
     * @param ticker    risk-factor name (e.g. "NVDA")
     * @param csvPath   path to the CSV file (local, HDFS, S3 …)
     * @param asOfDate  calibration reference date — only prices on or before this date are used
     * @return calibrated {@link MarketData} snapshot
     */
    public MarketData ingest(String ticker, String csvPath, LocalDate asOfDate) {
        log.info("Ingesting {} from '{}' as of {}", ticker, csvPath, asOfDate);

        Dataset<Row> raw = spark.read()
                .option("header", "true")
                .csv(csvPath);

        List<Double> prices = raw
                .select(col("Date"), col("Close").cast("double"))
                .filter(col("Date").leq(asOfDate.toString()))
                .sort(col("Date").asc())
                .collectAsList()
                .stream()
                .map(row -> row.getDouble(1))
                .toList();

        log.info("Loaded {} price observations for {} (up to {})", prices.size(), ticker, asOfDate);

        MarketData marketData = calibrationService.calibrateFromPrices(
                asOfDate, Map.of(ticker, prices));

        marketDataRepository.save(marketData);
        log.info("Market data saved — ticker={}, vol={}", ticker, marketData.getVolFor(ticker));
        return marketData;
    }

    /**
     * Reads every {TICKER}.csv file in {@code pricesDir}, calibrates a multi-ticker
     * {@link MarketData} snapshot using all prices on or before {@code asOfDate},
     * persists it, and returns it.
     *
     * @param pricesDir directory containing one CSV per ticker (e.g. NVDA.csv)
     * @param asOfDate  calibration reference date
     */
    public MarketData ingestDirectory(String pricesDir, LocalDate asOfDate) {
        log.info("Ingesting all tickers from '{}' as of {}", pricesDir, asOfDate);

        Map<String, List<Double>> tickerPrices = new LinkedHashMap<>();
        spark.read()
                .option("header", "true")
                .csv(pricesDir + "/*.csv")
                .select(
                        upper(regexp_extract(col("_metadata.file_path"), ".*/([^/\\.]+)\\.csv$", 1)).as("ticker"),
                        col("Date"),
                        col("Close").cast("double").as("close"))
                .filter(col("Date").leq(asOfDate.toString()))
                .filter(col("close").isNotNull())
                .sort("ticker", "Date")
                .toLocalIterator()
                .forEachRemaining(row -> tickerPrices
                        .computeIfAbsent(row.getString(0), k -> new ArrayList<>())
                        .add(row.getDouble(2)));

        log.info("Collected prices for {} ticker(s): {}", tickerPrices.size(), tickerPrices.keySet());

        MarketData marketData = calibrationService.calibrateFromPrices(asOfDate, tickerPrices);
        marketDataRepository.save(marketData);
        log.info("Market data saved for {} ticker(s) as of {}", tickerPrices.size(), asOfDate);
        return marketData;
    }
}
