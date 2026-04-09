package com.kacemrisk.market.application.port.out;

import com.kacemrisk.market.domain.model.Portfolio;

import java.time.LocalDate;

/**
 * Driven port — SPI for publishing VaR results to downstream systems
 * (e.g. a message bus, a reporting database, a dashboard).
 */
public interface VaRResultPublisher {

    /**
     * @param portfolio  the evaluated portfolio
     * @param asOfDate   the valuation date
     * @param alpha      confidence level used for the calculation
     * @param varAmount  computed VaR (positive loss)
     */
    void publish(Portfolio portfolio, LocalDate asOfDate, double alpha, double varAmount);
}

