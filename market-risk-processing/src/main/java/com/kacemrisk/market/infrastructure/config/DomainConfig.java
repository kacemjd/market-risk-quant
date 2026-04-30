package com.kacemrisk.market.infrastructure.config;

import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.application.service.FetchHistoricalPricesService;
import com.kacemrisk.market.application.service.VaRService;
import com.kacemrisk.market.domain.service.calibration.MarketDataCalibrationService;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.sql.SparkSession;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring wiring for framework-free domain services and Spark context helpers. */
@Configuration
public class DomainConfig {

    @Bean
    public CalibrateMarketDataUseCase calibrateMarketDataUseCase() {
        return new MarketDataCalibrationService();
    }

    @Bean
    public CalculateVaRUseCase calculateVaRUseCase() {
        return new VaRService();
    }

    /**
     * Provides a {@link JavaSparkContext} wrapping the singleton {@link SparkSession}, used by
     * {@code ComposeAdapter} to broadcast objects to executors.
     */
    @Bean
    public JavaSparkContext javaSparkContext(SparkSession sparkSession) {
        return JavaSparkContext.fromSparkContext(sparkSession.sparkContext());
    }

    @Bean
    public FetchHistoricalPricesUseCase fetchHistoricalPricesUseCase(
            HistoricalPriceProvider historicalPriceProvider) {
        return new FetchHistoricalPricesService(historicalPriceProvider);
    }
}
