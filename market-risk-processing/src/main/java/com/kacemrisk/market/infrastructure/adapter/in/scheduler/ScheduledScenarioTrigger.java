package com.kacemrisk.market.infrastructure.adapter.in.scheduler;

import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * EOD cron-based trigger — fires the full VaR pipeline on a schedule. Activated only when {@code
 * scenario.schedule.enabled=true}.
 *
 * <p>All pipeline parameters come from the {@code scenario.schedule.*} yml block, mirroring the
 * fields in {@link com.kacemrisk.market.infrastructure.model.ScenarioRequest}.
 */
@Slf4j
@ConditionalOnProperty(name = "scenario.schedule.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class ScheduledScenarioTrigger {

    @Value("${scenario.schedule.portfolio-path:data/portfolio.csv}")
    private String portfolioPath;

    @Value("${scenario.schedule.var-method:HISTORICAL}")
    private String varMethod;

    @Value("${scenario.schedule.confidence-level:0.99}")
    private double confidenceLevel;

    @Value("${scenario.schedule.historical-window:99}")
    private int historicalWindow;

    @Value("${scenario.schedule.num-paths:10000}")
    private int numPaths;

    @Value("${scenario.schedule.time-grid:GRID_53}")
    private String timeGrid;

    private final TriggerScenarioUseCase triggerScenarioUseCase;

    @Scheduled(cron = "${scenario.schedule.cron:0 0 18 * * MON-FRI}")
    public void runEod() {
        final LocalDate asOfDate = LocalDate.now();
        log.info("Scheduled EOD trigger fired | asOfDate={} | method={}", asOfDate, varMethod);
        ScenarioNotification notification =
                ScenarioNotification.builder()
                        .correlationId(UUID.randomUUID().toString())
                        .portfolioCsvPath(portfolioPath)
                        .asOfDate(asOfDate)
                        .varMethod(VaRMethod.valueOf(varMethod))
                        .confidenceLevel(confidenceLevel)
                        .historicalWindow(historicalWindow)
                        .numPaths(numPaths)
                        .timeGrid(MaturityGrid.valueOf(timeGrid))
                        .build();
        triggerScenarioUseCase.trigger(notification);
    }
}
