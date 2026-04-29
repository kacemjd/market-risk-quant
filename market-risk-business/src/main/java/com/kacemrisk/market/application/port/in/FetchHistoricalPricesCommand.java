package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.AssetClass;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;
import java.util.Map;

@Value
@Builder
public class FetchHistoricalPricesCommand {

    Map<String, AssetClass> tickers;
    LocalDate from;
    LocalDate to;
}
