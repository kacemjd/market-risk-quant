package com.kacemrisk.market.infrastructure.adapter.out.persistence;

import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.domain.model.MarketData;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@ConditionalOnProperty(name = "output.sink", havingValue = "questdb")
@RequiredArgsConstructor
public class QuestDbMarketDataRepository implements MarketDataRepository {

    private final JdbcTemplate jdbc;

    /**
     * Persists all volatility entries for the scenario in a single JDBC batch, reducing round-trips
     * from N_tickers to 1 per scenario save.
     */
    @Override
    public void save(MarketData marketData) {
        String asOfDate = marketData.getAsOfDate().toString();
        List<Map.Entry<String, Double>> entries =
                List.copyOf(marketData.getVolatilities().entrySet());

        jdbc.batchUpdate(
                "INSERT INTO market_calibration (as_of_date, ticker, volatility, ts) VALUES (?, ?,"
                        + " ?, systimestamp())",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, asOfDate);
                        ps.setString(2, entries.get(i).getKey());
                        ps.setDouble(3, entries.get(i).getValue());
                    }

                    @Override
                    public int getBatchSize() {
                        return entries.size();
                    }
                });

        log.info("Saved calibration batch — {} ticker(s) | asOfDate={}", entries.size(), asOfDate);
    }

    @Override
    public Optional<MarketData> findByDate(LocalDate asOfDate) {
        List<Map<String, Object>> rows =
                jdbc.queryForList(
                        """
                        SELECT ticker, volatility FROM market_calibration
                        WHERE as_of_date = ?
                        LATEST ON ts PARTITION BY ticker
                        """,
                        asOfDate.toString());

        if (rows.isEmpty()) return Optional.empty();

        List<String> tickers = rows.stream().map(r -> (String) r.get("ticker")).toList();
        Map<String, Double> vols =
                rows.stream()
                        .collect(
                                Collectors.toMap(
                                        r -> (String) r.get("ticker"),
                                        r -> (Double) r.get("volatility")));

        return Optional.of(
                MarketData.builder()
                        .asOfDate(asOfDate)
                        .riskFactors(tickers)
                        .volatilities(vols)
                        .build());
    }
}
