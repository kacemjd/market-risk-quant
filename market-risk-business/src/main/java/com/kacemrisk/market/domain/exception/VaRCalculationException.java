package com.kacemrisk.market.domain.exception;

/**
 * Raised when a VaR calculation cannot proceed due to missing or inconsistent input data.
 */
public class VaRCalculationException extends DomainException {

    public VaRCalculationException(String message) {
        super(message);
    }

    public VaRCalculationException(String message, Throwable cause) {
        super(message, cause);
    }
}

