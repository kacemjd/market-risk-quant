package workflow;

import application.service.MonteCarloVaRService;
import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.VaRResult;

public class MonteCarloVaRPipeline implements VaRPipeline {

    @Override
    public VaRResult execute(Portoflio portfolio, MarketData marketData, ScenarioNotification notification) {
        return MonteCarloVaRService.builder()
                .numPaths(notification.getNumPaths())
                .confidenceLevel(notification.getConfidenceLevel())
                .timeGrid(notification.getTimeGrid())
                .build()
                .runSimulation(portfolio, marketData);
    }
}

