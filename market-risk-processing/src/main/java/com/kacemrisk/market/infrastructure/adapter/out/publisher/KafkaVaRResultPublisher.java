package com.kacemrisk.market.infrastructure.adapter.out.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.domain.model.VaRResult;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@ConditionalOnBean(KafkaTemplate.class)
@RequiredArgsConstructor
public class KafkaVaRResultPublisher implements VaRResultPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${var.results.kafka.topic:var-results}")
    private String topic;

    @Override
    public void publish(String correlationId, Portfolio portfolio, LocalDate asOfDate,
                        VaRResult result, VaRMethod method) {
        Message msg = Message.builder()
                .correlationId(correlationId)
                .portfolioId(portfolio.getId())
                .asOfDate(asOfDate.toString())
                .method(method.name())
                .varAmount(result.getVar())
                .expectedShortfall(result.getExpectedShortfall())
                .alpha(result.getAlpha())
                .meanPnL(result.getMeanPnL())
                .stdDevPnL(result.getStdDevPnL())
                .build();
        try {
            kafkaTemplate.send(topic, correlationId, objectMapper.writeValueAsString(msg));
            log.info("VaR published to Kafka | topic={} | correlationId={} | portfolio={}",
                    topic, correlationId, portfolio.getId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize VaR result for Kafka", e);
        }
    }

    @Builder
    record Message(String correlationId, String portfolioId, String asOfDate, String method, double varAmount,
                   double expectedShortfall, double alpha, double meanPnL, double stdDevPnL) {
    }
}
