package com.kacemrisk.market.infrastructure.config;

import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.application.service.VaRService;
import com.kacemrisk.market.domain.service.calibration.MarketDataCalibrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.kacemrisk.market.workflow.VaRCalculationPipeline;
import com.kacemrisk.market.workflow.VaRPipeline;

@Configuration
public class DomainConfig {

    @Bean
    public MarketDataCalibrationService marketDataCalibrationService() {
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
}
