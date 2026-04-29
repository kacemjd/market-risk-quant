package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.VaRMethod;

import java.time.LocalDate;

/**
 * Execution metadata for one scenario run.
 *
 * <p>Built from {@link ScenarioNotification} at the start of
 * {@link com.kacemrisk.market.workflow.TriggerScenarioUseCase} and threaded through
 * the pipeline so that no downstream component needs to hold a reference to the
 * raw notification.
 */
public record RunContext(
        String correlationId,
        LocalDate asOfDate,
        VaRMethod varMethod,
        double confidenceLevel,
        int historicalWindow,
        int numPaths,
        MaturityGrid timeGrid
) {

    /**
     * Factory — extracts the execution-relevant fields from an inbound notification.
     */
    public static RunContext from(ScenarioNotification n) {
        return new RunContext(
                n.getCorrelationId(),
                n.getAsOfDate(),
                n.getVarMethod(),
                n.getConfidenceLevel(),
                n.getHistoricalWindow(),
                n.getNumPaths(),
                n.getTimeGrid());
    }
}

