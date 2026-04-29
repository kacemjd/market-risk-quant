package com.kacemrisk.market.infrastructure.adapter.in.rest;

import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.infrastructure.model.ScenarioRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/scenarios")
@RequiredArgsConstructor
@Tag(name = "Scenarios", description = "Trigger VaR scenario pipelines")
public class RestScenarioController {

    private final TriggerScenarioUseCase triggerScenarioUseCase;

    @Operation(summary = "Trigger VaR scenario", description = "Runs the full Monte Carlo + Parametric VaR pipeline for the given portfolio and returns a correlationId")
    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> run(@RequestBody ScenarioRequest request) {
        log.info("REST trigger received | asOfDate={}", request.getAsOfDate());
        ScenarioNotification notification = toNotification(request);
        String correlationId = triggerScenarioUseCase.trigger(notification);
        return Map.of("correlationId", correlationId);
    }

    private ScenarioNotification toNotification(ScenarioRequest r) {
        return ScenarioNotification.builder()
                .correlationId(UUID.randomUUID().toString())
                .portfolioCsvPath(r.getPortfolioCsvPath())
                .pricesCsvPath(r.getPricesCsvPath())
                .asOfDate(r.getAsOfDate())
                .confidenceLevel(r.getConfidenceLevel())
                .numPaths(r.getNumPaths())
                .timeGrid(MaturityGrid.valueOf(r.getTimeGrid()))
                .build();
    }
}
