package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.AssetClass;
import java.time.LocalDate;
import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FetchHistoricalPricesCommand {

    Map<String, AssetClass> tickers;
    LocalDate from;
    LocalDate to;
}
