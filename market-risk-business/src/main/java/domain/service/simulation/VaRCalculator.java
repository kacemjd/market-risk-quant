package domain.service.simulation;

import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.VaRResult;

public interface VaRCalculator {

    VaRResult calculate(Portoflio portoflio, MarketData marketData, double alpha);
}
