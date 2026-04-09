package com.kacemrisk.market.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScenarioRiskException {

    String errorCode;
    int    status;
    String error;
    String message;

    @Builder.Default
    Instant timestamp = Instant.now();

    List<ConstraintViolation> violations;

    public enum ErrorCode {
        SCENARIO_REQUEST_INVALID,
        SCENARIO_REQUEST_MALFORMED,
        SCENARIO_REQUEST_BAD_PARAMETER,
        VAR_CALCULATION_FAILED,
        RISK_PLATFORM_ERROR
    }

    @Value
    @Builder
    public static class ConstraintViolation {
        String field;
        String message;
    }
}

