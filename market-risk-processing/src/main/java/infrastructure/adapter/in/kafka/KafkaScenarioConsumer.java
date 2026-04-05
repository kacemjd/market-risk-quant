package infrastructure.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
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

/**
 * Inbound Kafka adapter — listens on {@code scenario-notifications} and delegates
 * to {@link TriggerScenarioUseCase}.
 *
 * <p>The consumer factory is configured with {@code StringDeserializer}; this class
 * owns the JSON → {@link ScenarioRequest} mapping via Jackson {@link ObjectMapper},
 * avoiding the Spring Kafka 4.x deprecated {@code JsonDeserializer}.
 */
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@Component
@RequiredArgsConstructor
public class KafkaScenarioConsumer {

    private final TriggerScenarioUseCase triggerScenarioUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics           = "${scenario.kafka.topic:scenario-notifications}",
            containerFactory = "scenarioListenerContainerFactory",
            groupId          = "${spring.kafka.consumer.group-id:market-risk-processing}"
    )
    public void onNotification(String message) {
        try {
            ScenarioRequest request = objectMapper.readValue(message, ScenarioRequest.class);
            log.info("Kafka trigger received | asOfDate={}", request.getAsOfDate());
            triggerScenarioUseCase.trigger(toNotification(request));
        } catch (Exception e) {
            log.error("Failed to deserialize or process Kafka scenario message", e);
        }
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
