package com.kacemrisk.market.domain.model;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/**
 * Immutable value object representing a single end-of-day closing price for a given instrument.
 * Produced by the Alpha Vantage adapter and consumed by calibration / VaR services.
 */
@Value
@Builder
public class HistoricalPrice {

    /** Instrument identifier (stock ticker, FX pair symbol, ETF symbol …). */
    String ticker;

    /** Business date the closing price refers to. */
    LocalDate date;

    /** Adjusted closing price in the instrument's native currency. */
    double closePrice;
}
