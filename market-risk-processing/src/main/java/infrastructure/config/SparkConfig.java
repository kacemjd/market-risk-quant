package infrastructure.config;

import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the Spark session.
 * The session is a singleton managed by the Spring context.
 */
@Configuration
public class SparkConfig {

    @Value("${spark.app-name:market-risk-processing}")
    private String appName;

    @Value("${spark.master:local[*]}")
    private String master;

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .appName(appName)
                .master(master)
                .getOrCreate();
    }
}

