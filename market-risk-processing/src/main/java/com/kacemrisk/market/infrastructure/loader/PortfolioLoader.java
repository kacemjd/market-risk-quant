package com.kacemrisk.market.infrastructure.loader;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.upper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PortfolioLoader {

    private final SparkSession spark;
    private final ResourceLoader resourceLoader;

    @Value("${input.portfolio.path:data/portfolio.csv}")
    private String portfolioPath;

    /** Explicit schema — avoids inferSchema scan and enforces column contract. */
    public static final StructType SCHEMA =
            new StructType()
                    .add("portfolioId", DataTypes.StringType, true)
                    .add("ticker", DataTypes.StringType, true)
                    .add("quantity", DataTypes.DoubleType, true)
                    .add("assetClass", DataTypes.StringType, true);

    /** Loads, normalises, and deduplicates the portfolio. */
    public Dataset<Row> load() {
        String path = resolveSparkPath(portfolioPath);
        log.info("[PortfolioLoader] Reading portfolio from '{}'", path);

        return spark.read()
                .option("header", "true")
                .schema(SCHEMA)
                .csv(path)
                .withColumn("ticker", upper(col("ticker")))
                .withColumn("assetClass", upper(col("assetClass")))
                .na()
                .drop()
                .dropDuplicates("portfolioId", "ticker");
    }

    /**
     * Resolves a config path to something Spark's {@code csv()} can open.
     *
     * <ul>
     *   <li>Cloud / HDFS / absolute → returned unchanged.
     *   <li>Classpath-relative → resolved to absolute filesystem path via Spring.
     * </ul>
     */
    private String resolveSparkPath(String path) {
        if (path.startsWith("s3://")
                || path.startsWith("hdfs://")
                || Paths.get(path).isAbsolute()) {
            return path;
        }
        Resource resource = resourceLoader.getResource("classpath:" + path);
        try {
            // Works in exploded builds and integration-test classpaths
            return resource.getFile().getAbsolutePath();
        } catch (IOException e) {
            // Inside a fat JAR the resource lives inside the archive and has no real File.
            // Extract it to a temp file so Spark can open it via the local filesystem.
            log.info(
                    "[PortfolioLoader] Resource '{}' is inside a JAR — extracting to temp file.",
                    path);
            try (InputStream in = resource.getInputStream()) {
                String suffix = path.contains(".") ? path.substring(path.lastIndexOf('.')) : ".tmp";
                Path tmp = Files.createTempFile("portfolio-", suffix);
                tmp.toFile().deleteOnExit();
                Files.copy(in, tmp, StandardCopyOption.REPLACE_EXISTING);
                log.info("[PortfolioLoader] Extracted to '{}'", tmp);
                return tmp.toAbsolutePath().toString();
            } catch (IOException ex) {
                log.warn(
                        "[PortfolioLoader] Cannot extract '{}' ({}); passing through.",
                        path,
                        ex.getMessage());
                return path;
            }
        }
    }
}
