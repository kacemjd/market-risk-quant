package application.port.in;

import domain.model.VaRResult;

/**
 * Driving port — entry point for triggering a VaR calculation.
 * Implementations live in the application service layer ({@code VaRService}).
 */
public interface CalculateVaRUseCase {

    /**
     * Executes the configured VaR methodology against the provided portfolio.
     *
     * @param command encapsulates the portfolio, market data, and simulation parameters
     * @return full VaR result (value, confidence, scenario stats, expected shortfall)
     */
    VaRResult calculate(CalculateVaRCommand command);
}

