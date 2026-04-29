package com.kacemrisk.market.application.port.in;

import com.kacemrisk.market.domain.model.Portfolio;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

/**
 * Command object that encapsulates all inputs required to trigger a bulk
 * historical price fetch for an entire portfolio.
 */
@Value
@Builder
public class FetchHistoricalPricesCommand {

    /** Portfolio whose positions will be fetched in bulk. */
    Portfolio portfolio;

    /** Start of the historical window (inclusive). */
    LocalDate from;

    /** End of the historical window (inclusive). */
    LocalDate to;
}

