package com.kacemrisk.market.infrastructure.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.domain.model.VaRResult;
import com.kacemrisk.market.infrastructure.RiskPlatformApplication;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * Full pipeline integration test.
 *
 * <p>Profile {@code int} activates {@code input.source=csv} pointing at {@code
 * src/test/resources/market-data/prices/} — no API key required. The portfolio is read from the
 * same classpath location. Only NVDA has fixture data, so only one enriched position is expected.
 */
@SpringBootTest(classes = RiskPlatformApplication.class)
@ActiveProfiles("int")
class ScenarioPipelineIT {

    @Autowired private TriggerScenarioUseCase triggerScenarioUseCase;
    @Autowired private MarketDataRepository marketDataRepository;

    @MockitoSpyBean private VaRResultPublisher varResultPublisher;

    @Test
    void should_run_full_var_pipeline_from_scenario_notification() {

        ScenarioNotification notification =
                ScenarioNotification.builder()
                        .correlationId(UUID.randomUUID().toString())
                        .portfolioCsvPath("market-data/portfolio.csv")
                        .asOfDate(LocalDate.of(2017, 11, 10))
                        .varMethod(VaRMethod.HISTORICAL)
                        .confidenceLevel(0.99)
                        .historicalWindow(50) // 50 trading-day window; NVDA fixture covers this
                        .numPaths(1000)
                        .timeGrid(MaturityGrid.GRID_53)
                        .build();

        // ── trigger ───────────────────────────────────────────────────────────
        String returnedId = triggerScenarioUseCase.trigger(notification);

        // ── correlationId round-trips ─────────────────────────────────────────
        assertThat(returnedId).isEqualTo(notification.getCorrelationId());

        // ── MarketData was calibrated and persisted ───────────────────────────
        MarketData marketData =
                marketDataRepository
                        .findByDate(notification.getAsOfDate())
                        .orElseThrow(
                                () ->
                                        new AssertionError(
                                                "MarketData not persisted for "
                                                        + notification.getAsOfDate()));

        assertThat(marketData.getAsOfDate()).isEqualTo(notification.getAsOfDate());
        assertThat(marketData.getRiskFactors()).contains("NVDA");
        assertThat(marketData.getVolFor("NVDA")).isGreaterThan(0.0);

        // ── VaR was published ─────────────────────────────────────────────────
        ArgumentCaptor<Portfolio> portfolioCaptor = ArgumentCaptor.forClass(Portfolio.class);
        ArgumentCaptor<VaRResult> varResultCaptor = ArgumentCaptor.forClass(VaRResult.class);

        verify(varResultPublisher)
                .publish(
                        eq(notification.getCorrelationId()),
                        portfolioCaptor.capture(),
                        eq(notification.getAsOfDate()),
                        varResultCaptor.capture(),
                        any(VaRMethod.class));

        assertThat(portfolioCaptor.getValue().getId()).isEqualTo("PTFL-001");
        assertThat(varResultCaptor.getValue().getVar())
                .as("VaR must be a positive loss amount")
                .isGreaterThan(0.0);
    }
}
