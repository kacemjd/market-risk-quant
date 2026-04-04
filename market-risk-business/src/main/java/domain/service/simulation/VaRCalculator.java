package domain.service.simulation;

import domain.model.MarketData;
import domain.model.Portoflio;

public interface VaRCalculator {

    double calculate(Portoflio portoflio, MarketData marketData, double alpha);
}
