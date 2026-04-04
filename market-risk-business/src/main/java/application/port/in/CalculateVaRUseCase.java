package application.port.in;

import domain.model.MarketData;
import domain.model.Portoflio;

/**
 * Driving port — entry point for triggering a VaR calculation.
 * Implementations live in the domain service layer.
 */
public interface CalculateVaRUseCase {

    /**
     * @param portfolio  the portfolio to evaluate
     * @param marketData calibrated market data (vol, covariance …)
     * @param alpha      confidence level, e.g. 0.95 or 0.99
     * @return VaR expressed as a positive loss amount
     */
    double calculate(Portoflio portfolio, MarketData marketData, double alpha);
}

