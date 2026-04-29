package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.HistoricalPrice;
import reactor.core.publisher.Flux;

public interface FetchHistoricalPricesUseCase {

    Flux<HistoricalPrice> fetchForPortfolio(FetchHistoricalPricesCommand command);
}

