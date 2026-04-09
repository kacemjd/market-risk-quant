package com.kacemrisk.market.infrastructure.adapter.out.persistence;

import com.kacemrisk.market.application.port.out.PortfolioRepository;
import com.kacemrisk.market.domain.model.Portfolio;
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

    private final Map<String, Portfolio> store = new ConcurrentHashMap<>();

    @Override
    public Optional<Portfolio> findById(String portfolioId) {
        return Optional.ofNullable(store.get(portfolioId));
    }

    @Override
    public List<Portfolio> findAll() {
        return new ArrayList<>(store.values());
    }

    public void save(Portfolio portfolio) {
        log.debug("Saving portfolio {}", portfolio.getId());
        store.put(portfolio.getId(), portfolio);
    }
}

