package com.kacemrisk.market.infrastructure.adapter.out.publisher;

import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRMethod;
import com.kacemrisk.market.domain.model.VaRResult;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Default (stdout) {@link VaRResultPublisher}. Active when {@code output.sink=log} or when the
 * property is absent entirely. No external services required — safe for local and CI environments.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "output.sink", havingValue = "log", matchIfMissing = true)
public class LoggingVaRResultPublisher implements VaRResultPublisher {

    @Override
    public void publish(
            String correlationId,
            Portfolio portfolio,
            LocalDate asOfDate,
            VaRResult result,
            VaRMethod method) {
        log.info(
                "VaR result | correlationId={} | portfolio={} | asOfDate={} | method={} | α={} |"
                        + " VaR={} | ES={}",
                correlationId,
                portfolio.getId(),
                asOfDate,
                method,
                result.getAlpha(),
                result.getVar(),
                result.getExpectedShortfall());
    }
}
