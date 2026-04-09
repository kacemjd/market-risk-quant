package com.kacemrisk.market.infrastructure.adapter.out.persistence;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("questdb")
@RequiredArgsConstructor
public class QuestDbSchemaInitializer {

    private final JdbcTemplate jdbc;

    @PostConstruct
    public void createTables() {
        log.info("QuestDB: initializing schema");

        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS var_results (
                    correlation_id  SYMBOL,
                    portfolio_id    SYMBOL,
                    as_of_date      SYMBOL,
                    var_amount      DOUBLE,
                    expected_shortfall DOUBLE,
                    alpha           DOUBLE,
                    mean_pnl        DOUBLE,
                    std_dev_pnl     DOUBLE,
                    num_scenarios   INT,
                    ts              TIMESTAMP
                ) TIMESTAMP(ts) PARTITION BY DAY WAL;
                """);

        jdbc.execute("""
                CREATE TABLE IF NOT EXISTS market_calibration (
                    as_of_date  SYMBOL,
                    ticker      SYMBOL,
                    volatility  DOUBLE,
                    ts          TIMESTAMP
                ) TIMESTAMP(ts) PARTITION BY DAY WAL;
                """);

        log.info("QuestDB: schema ready");
    }
}

