package infrastructure.config;

import domain.service.calibration.MarketDataCalibrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import workflow.MonteCarloVaRPipeline;
import workflow.VaRPipeline;

@Configuration
public class DomainConfig {

    @Bean
    public MarketDataCalibrationService marketDataCalibrationService() {
        return new MarketDataCalibrationService();
    }

    @Bean
    public VaRPipeline varPipeline() {
        return new MonteCarloVaRPipeline();
    }
}
