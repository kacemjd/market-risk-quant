package com.kacemrisk.market.infrastructure.adapter.out.publisher;

import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.domain.model.VaRResult;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "output.sink", havingValue = "questdb")
@RequiredArgsConstructor
public class QuestDbVaRResultPublisher implements VaRResultPublisher {

    private final JdbcTemplate jdbc;

    /** Per-result insert — used when results are published individually (pre-batch path). */
    @Override
    public void publish(
            String correlationId,
            Portfolio portfolio,
            LocalDate asOfDate,
            VaRResult result,
            VaRMethod method) {
        jdbc.update(
                """
INSERT INTO var_results
    (correlation_id, portfolio_id, as_of_date, method, var_amount, expected_shortfall,
     alpha, mean_pnl, std_dev_pnl, num_scenarios, ts)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, systimestamp())
""",
                correlationId,
                portfolio.getId(),
                asOfDate.toString(),
                method.name(),
                result.getVar(),
                result.getExpectedShortfall(),
                result.getAlpha(),
                result.getMeanPnL(),
                result.getStdDevPnL(),
                result.getNumberOfScenarios());

        log.info(
                "VaR persisted | correlationId={} | portfolio={} | method={} | VaR={} | ES={}",
                correlationId,
                portfolio.getId(),
                method,
                result.getVar(),
                result.getExpectedShortfall());
    }

    /**
     * Batch publish — issues all inserts in a single JDBC round-trip. Prefer this over calling
     * {@link #publish} in a loop at scenario scale.
     */
    public void publishBatch(
            String correlationId,
            List<PortfolioResultRow> results,
            LocalDate asOfDate,
            VaRMethod method) {
        if (results.isEmpty()) return;

        jdbc.batchUpdate(
                """
INSERT INTO var_results
    (correlation_id, portfolio_id, as_of_date, method, var_amount, expected_shortfall,
     alpha, mean_pnl, std_dev_pnl, num_scenarios, ts)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, systimestamp())
""",
                new BatchPreparedStatementSetter() {
                    @Override
                    public void setValues(@NonNull PreparedStatement ps, int i)
                            throws SQLException {
                        PortfolioResultRow row = results.get(i);
                        ps.setString(1, correlationId);
                        ps.setString(2, row.portfolioId());
                        ps.setString(3, asOfDate.toString());
                        ps.setString(4, method.name());
                        ps.setDouble(5, row.result().getVar());
                        ps.setDouble(6, row.result().getExpectedShortfall());
                        ps.setDouble(7, row.result().getAlpha());
                        ps.setDouble(8, row.result().getMeanPnL());
                        ps.setDouble(9, row.result().getStdDevPnL());
                        ps.setInt(10, row.result().getNumberOfScenarios());
                    }

                    @Override
                    public int getBatchSize() {
                        return results.size();
                    }
                });

        log.info(
                "VaR batch persisted | correlationId={} | {} portfolios | method={}",
                correlationId,
                results.size(),
                method);
    }

    /** Lightweight value type for batch publish. */
    public record PortfolioResultRow(String portfolioId, VaRResult result) {}
}
