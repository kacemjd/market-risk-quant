package infrastructure.config;

import application.port.in.CalculateVaRUseCase;
import application.service.VaRService;
import domain.service.calibration.MarketDataCalibrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import workflow.VaRCalculationPipeline;
import workflow.VaRPipeline;

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
