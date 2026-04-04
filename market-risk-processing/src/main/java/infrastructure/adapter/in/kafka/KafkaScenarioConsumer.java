package infrastructure.adapter.in.kafka;

import domain.model.MaturityGrid;
import infrastructure.model.ScenarioRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import workflow.ScenarioNotification;
import workflow.TriggerScenarioUseCase;

import java.util.UUID;

@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@Component
@RequiredArgsConstructor
public class KafkaScenarioConsumer {

    private final TriggerScenarioUseCase triggerScenarioUseCase;

    @KafkaListener(
            topics                = "${scenario.kafka.topic:scenario-notifications}",
            containerFactory      = "scenarioListenerContainerFactory",
            groupId               = "${spring.kafka.consumer.group-id:market-risk-processing}"
    )
    public void onNotification(ScenarioRequest request) {
        log.info("Kafka trigger received | asOfDate={}", request.getAsOfDate());
        triggerScenarioUseCase.trigger(toNotification(request));
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

