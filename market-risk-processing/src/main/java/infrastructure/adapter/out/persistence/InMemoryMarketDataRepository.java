package infrastructure.adapter.out.persistence;

import application.port.out.MarketDataRepository;
import domain.model.MarketData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Outbound adapter — trivial in-memory implementation of {@link MarketDataRepository}.
 * Replace with a database- or Spark-backed implementation for production.
 */
@Slf4j
@Repository
public class InMemoryMarketDataRepository implements MarketDataRepository {

    private final Map<LocalDate, MarketData> store = new ConcurrentHashMap<>();

    @Override
    public Optional<MarketData> findByDate(LocalDate asOfDate) {
        return Optional.ofNullable(store.get(asOfDate));
    }

    @Override
    public void save(MarketData marketData) {
        log.debug("Saving market data for {}", marketData.getAsOfDate());
        store.put(marketData.getAsOfDate(), marketData);
    }
}

