package infrastructure.adapter.out.persistence;

import application.port.out.PortfolioRepository;
import domain.model.Portoflio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outbound adapter — trivial in-memory implementation of {@link PortfolioRepository}.
 * Replace with a database- or Spark-backed implementation for production.
 */
@Slf4j
@Repository
public class InMemoryPortfolioRepository implements PortfolioRepository {

    private final Map<String, Portoflio> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Portoflio> findById(String portfolioId) {
        return Optional.ofNullable(store.get(portfolioId));
    }

    @Override
    public List<Portoflio> findAll() {
        return new ArrayList<>(store.values());
    }

    public void save(Portoflio portfolio) {
        log.debug("Saving portfolio {}", portfolio.getId());
        store.put(portfolio.getId(), portfolio);
    }
}

