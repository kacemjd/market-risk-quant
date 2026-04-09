package com.kacemrisk.market.infrastructure.adapter.in.rest;

import com.kacemrisk.market.domain.exception.DomainException;
import com.kacemrisk.market.domain.exception.VaRCalculationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ScenarioRiskException> handleValidation(MethodArgumentNotValidException ex) {
        List<ScenarioRiskException.ConstraintViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ScenarioRiskException.ConstraintViolation.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        String message = "Scenario request validation failed for %d field(s)".formatted(violations.size());
        log.warn("[{}] {}: {}", ScenarioRiskException.ErrorCode.SCENARIO_REQUEST_INVALID, message, violations);

        return ResponseEntity.badRequest().body(ScenarioRiskException.builder()
                .errorCode(ScenarioRiskException.ErrorCode.SCENARIO_REQUEST_INVALID.name())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .violations(violations)
                .build());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ScenarioRiskException> handleUnreadable(HttpMessageNotReadableException ex) {
        String message = "Malformed scenario request body: " + rootCause(ex);
        log.warn("[{}] {}", ScenarioRiskException.ErrorCode.SCENARIO_REQUEST_MALFORMED, message);

        return ResponseEntity.badRequest().body(ScenarioRiskException.builder()
                .errorCode(ScenarioRiskException.ErrorCode.SCENARIO_REQUEST_MALFORMED.name())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .build());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ScenarioRiskException> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[{}] {}", ScenarioRiskException.ErrorCode.SCENARIO_REQUEST_BAD_PARAMETER, ex.getMessage());

        return ResponseEntity.badRequest().body(ScenarioRiskException.builder()
                .errorCode(ScenarioRiskException.ErrorCode.SCENARIO_REQUEST_BAD_PARAMETER.name())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler({VaRCalculationException.class, DomainException.class})
    public ResponseEntity<ScenarioRiskException> handleDomainException(DomainException ex) {
        log.warn("[{}] {}", ScenarioRiskException.ErrorCode.VAR_CALCULATION_FAILED, ex.getMessage());

        return ResponseEntity.status(HttpStatusCode.valueOf(422)).body(ScenarioRiskException.builder()
                .errorCode(ScenarioRiskException.ErrorCode.VAR_CALCULATION_FAILED.name())
                .status(422)
                .error("Unprocessable Entity")
                .message(ex.getMessage())
                .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ScenarioRiskException> handleUnexpected(Exception ex) {
        log.error("[{}] unexpected error", ScenarioRiskException.ErrorCode.RISK_PLATFORM_ERROR, ex);

        return ResponseEntity.internalServerError().body(ScenarioRiskException.builder()
                .errorCode(ScenarioRiskException.ErrorCode.RISK_PLATFORM_ERROR.name())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected risk platform error occurred. Please contact grogu.")
                .build());
    }

    private static String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) cause = cause.getCause();
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}
