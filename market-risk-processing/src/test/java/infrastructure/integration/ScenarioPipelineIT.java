package infrastructure.integration;

import application.port.out.MarketDataRepository;
import application.port.out.VaRResultPublisher;
import domain.model.MarketData;
import domain.model.MaturityGrid;
import domain.model.Portoflio;
import infrastructure.RiskPlatformApplication;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import workflow.ScenarioNotification;
import workflow.TriggerScenarioUseCase;

import java.net.URL;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = RiskPlatformApplication.class)
@ActiveProfiles("int")
class ScenarioPipelineIT {

    // ── injected from application-int.yml ────────────────────────────────────
    @Value("${test.scenario.input-paths.portfolio}") private String portfolioCsvPath;
    @Value("${test.scenario.input-paths.prices}")    private String pricesCsvPath;
    @Value("${test.scenario.as-of-date}")            private String asOfDateStr;
    @Value("${test.scenario.confidence-level}")      private double confidenceLevel;
    @Value("${test.scenario.num-paths}")             private int    numPaths;
    @Value("${test.scenario.time-grid}")             private String timeGrid;

    @Autowired private TriggerScenarioUseCase triggerScenarioUseCase;
    @Autowired private MarketDataRepository   marketDataRepository;

    @MockitoSpyBean
    private VaRResultPublisher varResultPublisher;

    private ScenarioNotification notification;

    @BeforeEach
    void initNotification() throws Exception {
        notification = ScenarioNotification.builder()
                .correlationId(UUID.randomUUID().toString())
                .portfolioCsvPath(resolveTestPath(portfolioCsvPath))
                .pricesCsvPath(resolveTestPath(pricesCsvPath))
                .asOfDate(LocalDate.parse(asOfDateStr))
                .confidenceLevel(confidenceLevel)
                .numPaths(numPaths)
                .timeGrid(MaturityGrid.valueOf(timeGrid))
                .build();
    }

    @Test
    void should_run_full_var_pipeline_from_scenario_notification() throws Exception {

        // ── trigger full platform ─────────────────────────────────────────────
        String returnedId = triggerScenarioUseCase.trigger(notification);

        // ── correlationId round-trips ─────────────────────────────────────────
        assertThat(returnedId).isEqualTo(notification.getCorrelationId());

        // ── MarketData was calibrated and persisted ───────────────────────────
        MarketData marketData = marketDataRepository.findByDate(notification.getAsOfDate())
                .orElseThrow(() -> new AssertionError(
                        "MarketData not persisted for " + notification.getAsOfDate()));

        assertThat(marketData.getAsOfDate()).isEqualTo(notification.getAsOfDate());
        assertThat(marketData.getRiskFactors()).contains("NVDA");
        assertThat(marketData.getVolFor("NVDA")).isGreaterThan(0.0);

        // ── VaR was published for portfolio PTFL-001 ──────────────────────────
        ArgumentCaptor<Portoflio> portfolioCaptor = ArgumentCaptor.forClass(Portoflio.class);
        ArgumentCaptor<Double>    varCaptor       = ArgumentCaptor.forClass(Double.class);

        verify(varResultPublisher).publish(
                portfolioCaptor.capture(),
                eq(notification.getAsOfDate()),
                eq(notification.getConfidenceLevel()),
                varCaptor.capture());

        assertThat(portfolioCaptor.getValue().getId()).isEqualTo("PTFL-001");
        assertThat(varCaptor.getValue())
                .as("VaR must be a positive loss amount")
                .isGreaterThan(0.0);
    }

    private String resolveTestPath(String classpathRelative) throws Exception {
        URL resource = getClass().getClassLoader().getResource(classpathRelative);
        assertThat(resource).as("Test resource not found: " + classpathRelative).isNotNull();
        return Paths.get(resource.toURI()).toString();
    }
}
