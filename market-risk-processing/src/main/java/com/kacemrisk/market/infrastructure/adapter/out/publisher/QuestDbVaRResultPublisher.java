package com.kacemrisk.market.infrastructure.adapter.out.publisher;

import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@Profile("questdb")
@RequiredArgsConstructor
public class QuestDbVaRResultPublisher implements VaRResultPublisher {

    private final JdbcTemplate jdbc;

    @Override
    public void publish(String correlationId, Portfolio portfolio, LocalDate asOfDate, VaRResult result) {
        jdbc.update("""
                INSERT INTO var_results
                    (correlation_id, portfolio_id, as_of_date, var_amount, expected_shortfall,
                     alpha, mean_pnl, std_dev_pnl, num_scenarios, ts)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, systimestamp())
                """,
                correlationId,
                portfolio.getId(),
                asOfDate.toString(),
                result.getVar(),
                result.getExpectedShortfall(),
                result.getAlpha(),
                result.getMeanPnL(),
                result.getStdDevPnL(),
                result.getNumberOfScenarios());

        log.info("VaR persisted | correlationId={} | portfolio={} | asOfDate={} | VaR={} | ES={}",
                correlationId, portfolio.getId(), asOfDate, result.getVar(), result.getExpectedShortfall());
    }
}

