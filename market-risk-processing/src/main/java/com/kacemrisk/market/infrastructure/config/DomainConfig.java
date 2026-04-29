package com.kacemrisk.market.infrastructure.config;

import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.application.port.out.HistoricalPriceProvider;
import com.kacemrisk.market.application.service.FetchHistoricalPricesService;
import com.kacemrisk.market.application.service.VaRService;
import com.kacemrisk.market.domain.service.calibration.MarketDataCalibrationService;
import com.kacemrisk.market.workflow.VaRCalculationPipeline;
import com.kacemrisk.market.workflow.VaRPipeline;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

    @Bean
    public VaRPipeline varPipeline(CalculateVaRUseCase calculateVaRUseCase) {
        return new VaRCalculationPipeline(calculateVaRUseCase);
    }


    @Bean
    public FetchHistoricalPricesUseCase fetchHistoricalPricesUseCase(
            HistoricalPriceProvider historicalPriceProvider) {
        return new FetchHistoricalPricesService(historicalPriceProvider);
    }
}
