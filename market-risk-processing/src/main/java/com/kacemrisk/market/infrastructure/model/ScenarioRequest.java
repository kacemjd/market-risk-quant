package com.kacemrisk.market.infrastructure.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import lombok.*;

/**
 * Inbound request DTO — the single canonical input for any VaR pipeline execution.
 *
 * <p>All fields have sensible defaults so a caller may fire {@code POST /scenarios/run} with an
 * empty {@code {}} body and receive a valid Historical VaR run against today's date.
 *
 * <ul>
 *   <li>{@code portfolioCsvPath} — classpath-relative path to the portfolio CSV
 *   <li>{@code asOfDate} — defaults to today when omitted
 *   <li>{@code varMethod} — HISTORICAL | MONTE_CARLO | PARAMETRIC
 *   <li>{@code historicalWindow} — number of trading-day returns used (drives both the calendar
 *       lookback and the scenario window)
 *   <li>{@code numPaths} — Monte Carlo paths; ignored for other methods
 *   <li>{@code timeGrid} — maturity grid token, e.g. {@code GRID_53}
 * </ul>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioRequest {

    /** Classpath-relative path to the portfolio CSV. */
    @Builder.Default private String portfolioCsvPath = "data/portfolio.csv";

    /**
     * As-of date for the VaR calculation. When {@code null} the handler defaults to {@code
     * LocalDate.now()}.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate asOfDate;

    /** VaR calculation method: HISTORICAL, MONTE_CARLO, or PARAMETRIC. */
    @Pattern(
            regexp = "HISTORICAL|MONTE_CARLO|PARAMETRIC",
            message = "varMethod must be HISTORICAL, MONTE_CARLO, or PARAMETRIC")
    @Builder.Default
    private String varMethod = "HISTORICAL";

    @DecimalMin(value = "0.5", inclusive = true, message = "confidenceLevel must be ≥ 0.5")
    @DecimalMax(value = "0.9999", inclusive = true, message = "confidenceLevel must be ≤ 0.9999")
    @Builder.Default
    private double confidenceLevel = 0.99;

    /**
     * Number of trading-day log-returns used as the scenario window. The price-load lookback is
     * derived as {@code historicalWindow × 2} calendar days (buffer to guarantee enough trading
     * days regardless of holidays).
     */
    @Min(value = 2, message = "historicalWindow must be ≥ 2")
    @Max(value = 2520, message = "historicalWindow must be ≤ 2520 (10 trading years)")
    @Builder.Default
    private int historicalWindow = 99;

    /** Number of Monte Carlo simulation paths — used only by MONTE_CARLO. */
    @Min(value = 1, message = "numPaths must be ≥ 1")
    @Max(value = 1_000_000, message = "numPaths must be ≤ 1 000 000")
    @Builder.Default
    private int numPaths = 10_000;

    @NotBlank(message = "timeGrid must not be blank")
    @Pattern(regexp = "GRID_\\d+", message = "timeGrid must match GRID_<N> (e.g. GRID_53)")
    @Builder.Default
    private String timeGrid = "GRID_53";
}
