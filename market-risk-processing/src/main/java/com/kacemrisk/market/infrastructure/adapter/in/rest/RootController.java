package com.kacemrisk.market.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root endpoint — provides a basic info/health response for browser access
 * and health checks when running with the servlet profile.
 */
@RestController
@Tag(name = "Health", description = "Application health and info")
public class RootController {

    @Operation(summary = "Health check", description = "Returns application status and available endpoints")
    @GetMapping("/")
    public Map<String, Object> health() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("status", "UP");
        info.put("app", "market-risk-processing");
        info.put("timestamp", Instant.now().toString());
        info.put("endpoints", Map.of(
                "health", "GET  /",
                "trigger run", "POST /scenarios/run",
                "var results", "GET  /results",
                "historical var", "GET  /results/history/{portfolioId}?from=&to=&method=MONTE_CARLO",
                "swagger ui", "GET  /swagger-ui.html",
                "spark ui", "http://localhost:4040"
        ));
        return info;
    }
}
