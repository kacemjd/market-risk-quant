package workflow;

import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.VaRResult;

public interface VaRPipeline {

    VaRResult execute(Portoflio portfolio, MarketData marketData, ScenarioNotification notification);
}

