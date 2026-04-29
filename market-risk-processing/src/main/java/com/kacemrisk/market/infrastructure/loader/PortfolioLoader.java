package com.kacemrisk.market.infrastructure.loader;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Paths;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.upper;

/**
 * Loads the portfolio CSV into a Spark {@link Dataset} using
 * {@code spark.read().csv(path)} — Spark owns the parsing.
 *
 * <p>The file path is resolved from {@code input.portfolio.path}:
 * <ul>
 *   <li>Absolute or cloud paths ({@code s3://}, {@code hdfs://}) are passed straight
 *       to Spark so it can use its native connectors.</li>
 *   <li>Classpath-relative paths (e.g. {@code data/portfolio.csv}) are resolved to the
 *       real filesystem path via Spring's {@link ResourceLoader} — this works for both
 *       exploded builds and integration-test classpaths.</li>
 * </ul>
 *
 * <p>The returned {@link Dataset} is normalised (upper-case ticker + assetClass),
 * null rows are dropped, and (portfolioId, ticker) pairs are deduplicated.
 * Callers are responsible for {@code .cache()} / {@code .unpersist()} if the
 * dataset is consumed more than once.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioLoader {

    private final SparkSession spark;
    private final ResourceLoader resourceLoader;

    @Value("${input.portfolio.path:data/portfolio.csv}")
    private String portfolioPath;

    /**
     * Explicit schema — avoids inferSchema scan and enforces column contract.
     */
    public static final StructType SCHEMA = new StructType()
            .add("portfolioId", DataTypes.StringType, true)
            .add("ticker", DataTypes.StringType, true)
            .add("quantity", DataTypes.DoubleType, true)
            .add("assetClass", DataTypes.StringType, true);

    /**
     * Loads, normalises, and deduplicates the portfolio.
     */
    public Dataset<Row> load() {
        String path = resolveSparkPath(portfolioPath);
        log.info("[PortfolioLoader] Reading portfolio from '{}'", path);

        return spark.read()
                .option("header", "true")
                .schema(SCHEMA)
                .csv(path)
                .withColumn("ticker", upper(col("ticker")))
                .withColumn("assetClass", upper(col("assetClass")))
                .na().drop()
                .dropDuplicates("portfolioId", "ticker");
    }

    /**
     * Resolves a config path to something Spark's {@code csv()} can open.
     *
     * <ul>
     *   <li>Cloud / HDFS / absolute → returned unchanged.</li>
     *   <li>Classpath-relative      → resolved to absolute filesystem path via Spring.</li>
     * </ul>
     */
    private String resolveSparkPath(String path) {
        if (path.startsWith("s3://") || path.startsWith("hdfs://") || Paths.get(path).isAbsolute()) {
            return path;
        }
        try {
            return resourceLoader.getResource("classpath:" + path)
                    .getFile()
                    .getAbsolutePath();
        } catch (IOException e) {
            log.warn("[PortfolioLoader] Cannot resolve '{}' to filesystem path ({}); passing through.",
                    path, e.getMessage());
            return path;
        }
    }
}

