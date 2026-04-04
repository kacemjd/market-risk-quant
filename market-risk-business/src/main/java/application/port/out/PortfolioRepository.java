package application.port.out;

import domain.model.Portoflio;

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
    Optional<Portoflio> findById(String portfolioId);

    /**
     * @return all available portfolios
     */
    List<Portoflio> findAll();
}

