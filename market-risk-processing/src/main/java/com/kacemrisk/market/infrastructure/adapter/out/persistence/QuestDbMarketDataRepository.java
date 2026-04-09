package com.kacemrisk.market.infrastructure.adapter.out.persistence;

import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.domain.model.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Profile("questdb")
@RequiredArgsConstructor
public class QuestDbMarketDataRepository implements MarketDataRepository {

    private final JdbcTemplate jdbc;

    @Override
    public void save(MarketData marketData) {
        String asOfDate = marketData.getAsOfDate().toString();
        marketData.getVolatilities().forEach((ticker, vol) -> {
            jdbc.update("""
                    INSERT INTO market_calibration (as_of_date, ticker, volatility, ts)
                    VALUES (?, ?, ?, systimestamp())
                    """, asOfDate, ticker, vol);
            log.debug("Saved calibration — ticker={} vol={} asOfDate={}", ticker, vol, asOfDate);
        });
    }

    @Override
    public Optional<MarketData> findByDate(LocalDate asOfDate) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT ticker, volatility FROM market_calibration
                WHERE as_of_date = ?
                LATEST ON ts PARTITION BY ticker
                """, asOfDate.toString());

        if (rows.isEmpty()) return Optional.empty();

        List<String> tickers = rows.stream().map(r -> (String) r.get("ticker")).toList();
        Map<String, Double> vols = rows.stream()
                .collect(Collectors.toMap(r -> (String) r.get("ticker"), r -> (Double) r.get("volatility")));

        return Optional.of(MarketData.builder()
                .asOfDate(asOfDate)
                .riskFactors(tickers)
                .volatilities(vols)
                .build());
    }
}

