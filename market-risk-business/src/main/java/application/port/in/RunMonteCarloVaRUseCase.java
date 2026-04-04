package application.port.in;

import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.VaRResult;

public interface RunMonteCarloVaRUseCase {

    VaRResult runSimulation(Portoflio portfolio, MarketData marketData);
}

