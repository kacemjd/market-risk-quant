package com.kacemrisk.market.infrastructure.model;

import com.fasterxml.jackson.annotation.JsonFormat;
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

    private String portfolioCsvPath;
    private String pricesCsvPath;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    private LocalDate asOfDate;

    @Builder.Default private double confidenceLevel = 0.99;
    @Builder.Default private int    numPaths        = 10_000;
    @Builder.Default private String timeGrid        = "GRID_53";
}

