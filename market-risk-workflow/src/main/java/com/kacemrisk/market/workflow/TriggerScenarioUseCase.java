package com.kacemrisk.market.workflow;

public interface TriggerScenarioUseCase {

    /** Triggers the full VaR pipeline for the given notification. Returns the correlationId. */
    String trigger(ScenarioNotification notification);
}

