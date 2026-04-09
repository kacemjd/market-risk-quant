package com.kacemrisk.market.infrastructure.adapter.out.publisher;

import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@Profile("!questdb")
public class LoggingVaRResultPublisher implements VaRResultPublisher {

    @Override
    public void publish(String correlationId, Portfolio portfolio, LocalDate asOfDate, VaRResult result) {
        log.info("VaR result | correlationId={} | portfolio={} | asOfDate={} | α={} | VaR={} | ES={}",
                correlationId, portfolio.getId(), asOfDate,
                result.getAlpha(), result.getVar(), result.getExpectedShortfall());
    }
}

