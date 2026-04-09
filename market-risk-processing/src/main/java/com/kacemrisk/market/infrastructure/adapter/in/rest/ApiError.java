package com.kacemrisk.market.infrastructure.adapter.in.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

/**
 * Uniform error envelope returned by {@link GlobalExceptionHandler}.
 *
 * <pre>
 * {
 *   "status"    : 400,
 *   "error"     : "Bad Request",
 *   "message"   : "Validation failed for 2 field(s)",
 *   "timestamp" : "2026-04-09T18:00:00Z",
 *   "violations": [
 *     { "field": "portfolioCsvPath", "message": "must not be blank" },
 *     { "field": "asOfDate",         "message": "must not be null"  }
 *   ]
 * }
 * </pre>
 */
@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    int    status;
    String error;
    String message;

    @Builder.Default
    Instant timestamp = Instant.now();

    /** Non-null only for 400 validation errors — one entry per failed constraint. */
    List<FieldViolation> violations;

    @Value
    @Builder
    public static class FieldViolation {
        String field;
        String message;
    }
}

