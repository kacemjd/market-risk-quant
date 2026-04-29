package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.Portfolio;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class FetchHistoricalPricesCommand {

    Portfolio portfolio;

    LocalDate from;

    LocalDate to;
}

