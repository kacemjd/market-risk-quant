package com.kacemrisk.market.infrastructure.adapter.in.rest;

import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.infrastructure.model.ScenarioRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/scenarios")
@RequiredArgsConstructor
@Tag(name = "Scenarios", description = "Trigger VaR scenario pipelines")
public class RestScenarioController {

    private final TriggerScenarioUseCase triggerScenarioUseCase;

    @Operation(
            summary = "Trigger VaR scenario",
            description = """
                    Runs the full VaR pipeline for the portfolio defined in the request.
                    All fields are optional — an empty `{}` body runs a Historical VaR
                    against today's date with the default portfolio and parameters.
                    Returns a correlationId that can be used to query results.
                    """)
    @PostMapping("/run")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Map<String, String> run(@RequestBody(required = false) ScenarioRequest request) {
        if (request == null) request = new ScenarioRequest();
        log.info("REST trigger | method={} | asOfDate={} | portfolio={}",
                request.getVarMethod(),
                request.getAsOfDate() != null ? request.getAsOfDate() : "today",
                request.getPortfolioCsvPath());
        String correlationId = triggerScenarioUseCase.trigger(toNotification(request));
        return Map.of("correlationId", correlationId);
    }

    private ScenarioNotification toNotification(ScenarioRequest r) {
        return ScenarioNotification.builder()
                .correlationId(UUID.randomUUID().toString())
                .portfolioCsvPath(r.getPortfolioCsvPath())
                .asOfDate(r.getAsOfDate() != null ? r.getAsOfDate() : LocalDate.now())
                .varMethod(VaRMethod.valueOf(r.getVarMethod()))
                .confidenceLevel(r.getConfidenceLevel())
                .historicalWindow(r.getHistoricalWindow())
                .numPaths(r.getNumPaths())
                .timeGrid(MaturityGrid.valueOf(r.getTimeGrid()))
                .build();
    }
}
