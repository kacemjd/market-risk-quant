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

/**
 * Centralised HTTP error mapping for the REST adapter layer.
 *
 * <table>
 *   <tr><th>Exception</th><th>HTTP status</th><th>When</th></tr>
 *   <tr><td>{@link MethodArgumentNotValidException}</td><td>400</td>
 *       <td>Bean Validation fails on {@code @Valid @RequestBody}</td></tr>
 *   <tr><td>{@link HttpMessageNotReadableException}</td><td>400</td>
 *       <td>Malformed JSON or unparseable date format</td></tr>
 *   <tr><td>{@link IllegalArgumentException}</td><td>400</td>
 *       <td>Unknown enum value (e.g. invalid {@code timeGrid})</td></tr>
 *   <tr><td>{@link VaRCalculationException}</td><td>422</td>
 *       <td>Domain rejects inputs as inconsistent (missing data, singular matrix…)</td></tr>
 *   <tr><td>{@link DomainException}</td><td>422</td>
 *       <td>Any other domain-level rule violation</td></tr>
 *   <tr><td>{@link Exception}</td><td>500</td>
 *       <td>Unexpected infrastructure or runtime failure</td></tr>
 * </table>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ── 400 Bad Request ──────────────────────────────────────────────────────

    /**
     * Fires when {@code @Valid} rejects one or more fields on the request body.
     * Returns one {@link ApiError.FieldViolation} per failed constraint.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<ApiError.FieldViolation> violations = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fe -> ApiError.FieldViolation.builder()
                        .field(fe.getField())
                        .message(fe.getDefaultMessage())
                        .build())
                .toList();

        String message = "Validation failed for %d field(s)".formatted(violations.size());
        log.warn("REST 400 — {}: {}", message, violations);

        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .violations(violations)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Fires when the request body cannot be parsed — e.g. invalid JSON syntax or
     * a date string that does not match {@code yyyy-MM-dd}.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
        String message = "Malformed request body: " + rootCause(ex);
        log.warn("REST 400 — {}", message);

        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(message)
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Fires for bad enum values, e.g. {@code MaturityGrid.valueOf("UNKNOWN")}.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("REST 400 — {}", ex.getMessage());

        ApiError body = ApiError.builder()
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .message(ex.getMessage())
                .build();

        return ResponseEntity.badRequest().body(body);
    }

    // ── 422 Unprocessable Entity ─────────────────────────────────────────────

    /**
     * Fires when the domain rejects the inputs as semantically invalid
     * (missing market data, singular covariance matrix, insufficient history …).
     */
    @ExceptionHandler({VaRCalculationException.class, DomainException.class})
    public ResponseEntity<ApiError> handleDomainException(DomainException ex) {
        log.warn("REST 422 — domain rule violation: {}", ex.getMessage());

        ApiError body = ApiError.builder()
                .status(422)
                .error("Unprocessable Entity")
                .message(ex.getMessage())
                .build();

        return ResponseEntity.status(HttpStatusCode.valueOf(422)).body(body);
    }

    // ── 500 Internal Server Error ────────────────────────────────────────────

    /**
     * Catch-all for unexpected infrastructure or runtime failures.
     * The internal cause is logged but NOT leaked to the caller.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
        log.error("REST 500 — unexpected error", ex);

        ApiError body = ApiError.builder()
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .error(HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
                .message("An unexpected error occurred. Please contact support.")
                .build();

        return ResponseEntity.internalServerError().body(body);
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private static String rootCause(Throwable t) {
        Throwable cause = t;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
    }
}

