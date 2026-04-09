package com.kacemrisk.market.infrastructure.adapter.out.publisher;

import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.domain.model.VaRResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class LoggingVaRResultPublisher implements VaRResultPublisher {

    @Override
    public void publish(String correlationId, Portfolio portfolio, LocalDate asOfDate,
                        VaRResult result, VaRMethod method) {
        log.info("VaR result | correlationId={} | portfolio={} | asOfDate={} | method={} | α={} | VaR={} | ES={}",
                correlationId, portfolio.getId(), asOfDate, method,
                result.getAlpha(), result.getVar(), result.getExpectedShortfall());
    }
}

