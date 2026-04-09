package com.kacemrisk.market.infrastructure.config;

import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.application.service.VaRService;
import com.kacemrisk.market.domain.service.calibration.MarketDataCalibrationService;
import com.kacemrisk.market.domain.service.calibration.MarketDataCalibrator;
import com.kacemrisk.market.workflow.VaRCalculationPipeline;
import com.kacemrisk.market.workflow.VaRPipeline;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DomainConfig {

    @Bean
    public MarketDataCalibrationService marketDataCalibrationService() {
        return new MarketDataCalibrationService();
    }

    @Bean
    public CalibrateMarketDataUseCase calibrateMarketDataUseCase(MarketDataCalibrationService calibrationService) {
        return new MarketDataCalibrator(calibrationService);
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
