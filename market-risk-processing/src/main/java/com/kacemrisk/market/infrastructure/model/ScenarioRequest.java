package com.kacemrisk.market.infrastructure.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/** Mutable request DTO — used by REST controller and Kafka consumer for JSON deserialization. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScenarioRequest {

    @NotBlank(message = "portfolioCsvPath must not be blank")
    private String portfolioCsvPath;

    @NotBlank(message = "pricesCsvPath must not be blank")
    private String pricesCsvPath;

    @NotNull(message = "asOfDate must not be null (expected format: yyyy-MM-dd)")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate asOfDate;

    @DecimalMin(value = "0.5",  inclusive = true,  message = "confidenceLevel must be ≥ 0.5")
    @DecimalMax(value = "0.9999", inclusive = true, message = "confidenceLevel must be ≤ 0.9999")
    @Builder.Default private double confidenceLevel = 0.99;

    @Min(value = 1,         message = "numPaths must be ≥ 1")
    @Max(value = 1_000_000, message = "numPaths must be ≤ 1 000 000")
    @Builder.Default private int numPaths = 10_000;

    @NotBlank(message = "timeGrid must not be blank")
    @Pattern(regexp = "GRID_\\d+", message = "timeGrid must match GRID_<N> (e.g. GRID_53)")
    @Builder.Default private String timeGrid = "GRID_53";
}
