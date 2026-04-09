package com.kacemrisk.market.infrastructure.adapter.in.scheduler;

import com.kacemrisk.market.domain.model.MaturityGrid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;

import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@ConditionalOnProperty(name = "scenario.schedule.enabled", havingValue = "true")
@Component
@RequiredArgsConstructor
public class ScheduledScenarioTrigger {

    @Value("${scenario.schedule.default-portfolio-path}")
    private String defaultPortfolioPath;

    @Value("${scenario.schedule.default-prices-path}")
    private String defaultPricesPath;

    @Value("${scenario.schedule.default-confidence-level:0.99}")
    private double defaultConfidenceLevel;

    @Value("${scenario.schedule.default-num-paths:10000}")
    private int defaultNumPaths;

    private final TriggerScenarioUseCase triggerScenarioUseCase;

    @Scheduled(cron = "${scenario.schedule.cron:0 0 18 * * MON-FRI}")
    public void runEod() {
        LocalDate asOfDate = LocalDate.now();
        log.info("Scheduled EOD trigger fired | asOfDate={}", asOfDate);
        ScenarioNotification notification = ScenarioNotification.builder()
                .correlationId(UUID.randomUUID().toString())
                .portfolioCsvPath(defaultPortfolioPath)
                .pricesCsvPath(defaultPricesPath)
                .asOfDate(asOfDate)
                .confidenceLevel(defaultConfidenceLevel)
                .numPaths(defaultNumPaths)
                .timeGrid(MaturityGrid.GRID_53)
                .build();
        triggerScenarioUseCase.trigger(notification);
    }
}

