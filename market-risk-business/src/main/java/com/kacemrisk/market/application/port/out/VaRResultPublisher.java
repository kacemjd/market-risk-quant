package com.kacemrisk.market.application.port.out;

import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;

import java.time.LocalDate;

public interface VaRResultPublisher {

    void publish(String correlationId, Portfolio portfolio, LocalDate asOfDate, VaRResult result);
}

