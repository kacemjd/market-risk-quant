package com.kacemrisk.market.domain.exception;

/**
 * Root unchecked exception for all domain-level errors.
 * Keeps the domain free of infrastructure concerns.
 */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }

    public DomainException(String message, Throwable cause) {
        super(message, cause);
    }
}

