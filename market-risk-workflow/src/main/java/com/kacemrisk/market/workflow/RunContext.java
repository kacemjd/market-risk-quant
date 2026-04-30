package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.VaRMethod;
import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

/**
 * Execution metadata for one scenario run.
 *
 * <p>Built from {@link ScenarioNotification} at the start of {@link
 * com.kacemrisk.market.workflow.TriggerScenarioUseCase} and threaded through the pipeline so that
 * no downstream component needs to hold a reference to the raw notification.
 *
 * <p>Implements {@link Serializable} so it can be broadcast to Spark executors.
 */
public record RunContext(
        String correlationId,
        LocalDate asOfDate,
        VaRMethod varMethod,
        double confidenceLevel,
        int historicalWindow,
        int numPaths,
        MaturityGrid timeGrid)
        implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    /** Factory — extracts the execution-relevant fields from an inbound notification. */
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
