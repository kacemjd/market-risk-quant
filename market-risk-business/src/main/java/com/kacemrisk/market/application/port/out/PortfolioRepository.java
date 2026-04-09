package com.kacemrisk.market.application.port.out;

import com.kacemrisk.market.domain.model.Portfolio;

import java.util.List;
import java.util.Optional;

/**
 * Driven port — SPI that infrastructure adapters must implement to
 * provide portfolio data to the domain.
 */
public interface PortfolioRepository {

    /**
     * @param portfolioId unique portfolio identifier
     * @return an {@link Optional} containing the portfolio if found
     */
    Optional<Portfolio> findById(String portfolioId);

    /**
     * @return all available portfolios
     */
    List<Portfolio> findAll();
}

