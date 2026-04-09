package com.kacemrisk.market.infrastructure.adapter.out.publisher;

import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Outbound adapter — logs VaR results.
 * Replace or extend with a messaging / reporting implementation for production
 * (e.g. Kafka producer, database writer, REST call).
 */
@Slf4j
@Component
public class LoggingVaRResultPublisher implements VaRResultPublisher {

    @Override
    public void publish(Portfolio portfolio, LocalDate asOfDate, double alpha, double varAmount) {
        log.info(
            "VaR result | portfolio={} | asOfDate={} | alpha={} | VaR={}",
            portfolio.getId(), asOfDate, alpha, varAmount
        );
        // TODO: replace with Kafka/DB/REST outbound call
    }
}

