package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.HistoricalPrice;

import java.util.List;

public interface FetchHistoricalPricesUseCase {

    List<HistoricalPrice> fetch(FetchHistoricalPricesCommand command);
}
