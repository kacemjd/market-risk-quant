package com.kacemrisk.market.infrastructure;

import com.kacemrisk.market.infrastructure.adapter.in.spark.ComposeAdapter;
import com.kacemrisk.market.infrastructure.adapter.in.spark.EnrichedDataset;
import com.kacemrisk.market.infrastructure.adapter.in.spark.JoinAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.kacemrisk.market.workflow.RunContext;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;

/**
 * Pure trigger — translates an inbound {@link ScenarioNotification} into a pipeline run.
 *
 * <p>No business logic here: join produces an {@link EnrichedDataset},
 * compose drives calibration (once) + VaR (per portfolio) + publishing.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioNotificationHandler implements TriggerScenarioUseCase {

    private final JoinAdapter joinAdapter;
    private final ComposeAdapter composeAdapter;

    @Override
    public String trigger(ScenarioNotification notification) {
        log.info(">>> Scenario [{}] | asOfDate={} | method={} | window={} | α={}",
                notification.getCorrelationId(), notification.getAsOfDate(),
                notification.getVarMethod(), notification.getHistoricalWindow(),
                notification.getConfidenceLevel());

        RunContext ctx = RunContext.from(notification);
        EnrichedDataset enriched = joinAdapter.enrich(ctx);
        composeAdapter.compute(enriched, ctx);

        log.info("<<< Scenario [{}] completed", ctx.correlationId());
        return ctx.correlationId();
    }
}
