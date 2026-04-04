package infrastructure.adapter.in.rest;

import domain.model.MaturityGrid;
import infrastructure.model.ScenarioRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import workflow.ScenarioNotification;
import workflow.TriggerScenarioUseCase;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Profile("rest")
@RestController
@RequestMapping("/scenarios")
@RequiredArgsConstructor
public class RestScenarioController {

    private final TriggerScenarioUseCase triggerScenarioUseCase;

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

