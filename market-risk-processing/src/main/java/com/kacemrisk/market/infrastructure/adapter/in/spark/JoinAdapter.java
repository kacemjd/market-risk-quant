package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.infrastructure.model.EnrichedPositionRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.expressions.Window;
import org.apache.spark.sql.expressions.WindowSpec;
import org.springframework.stereotype.Component;
import com.kacemrisk.market.workflow.ScenarioNotification;

import java.time.LocalDate;

import static org.apache.spark.sql.functions.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class JoinAdapter {

    private final SparkSession spark;

    /**
     * Reads portfolio positions and price files, joins on ticker to attach the latest
     * spot price, and returns an enriched dataset ready for the compute pipeline.
     *
     * Portfolio CSV schema : portfolioId, ticker, quantity, assetClass
     * Prices directory     : one file per ticker named {TICKER}.csv
     */
    public Dataset<EnrichedPositionRow> enrich(ScenarioNotification notification) {
        log.info("Enriching positions | portfolio={} | pricesDir={} | asOf={}",
                notification.getPortfolioCsvPath(),
                notification.getPricesCsvPath(),
                notification.getAsOfDate());

        Dataset<Row> portfolio = spark.read()
                .option("header", "true")
                .csv(notification.getPortfolioCsvPath())
                .select(
                        col("portfolioId"),
                        col("ticker"),
                        col("quantity").cast("double"),
                        col("assetClass"));

        Dataset<Row> latestPrices = latestClosePricePerTicker(
                notification.getPricesCsvPath(), notification.getAsOfDate());

        Dataset<Row> joined = portfolio.join(latestPrices, "ticker");
        log.info("Enriched {} position row(s)", joined.count());
        return joined.as(Encoders.bean(EnrichedPositionRow.class));
    }

    private Dataset<Row> latestClosePricePerTicker(String pricesDir, LocalDate asOfDate) {
        WindowSpec latestPerTicker = Window.partitionBy("ticker").orderBy(col("Date").desc());
        return spark.read()
                .option("header", "true")
                .csv(pricesDir + "/*.csv")
                .select(
                        upper(regexp_extract(col("_metadata.file_path"), ".*/([^/\\.]+)\\.csv$", 1)).as("ticker"),
                        col("Date"),
                        col("Close").cast("double").as("spotPrice"))
                .filter(col("Date").leq(asOfDate.toString()))
                .filter(col("spotPrice").isNotNull())
                .withColumn("rn", row_number().over(latestPerTicker))
                .filter(col("rn").equalTo(1))
                .select("ticker", "spotPrice");
    }
}
