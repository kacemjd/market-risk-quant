package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.MaturityGrid;
import com.kacemrisk.market.domain.model.VaRMethod;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDate;

@Value
@Builder
public class ScenarioNotification {

    String     correlationId;
    /** Classpath-relative path to the portfolio CSV (e.g. {@code "data/portfolio.csv"}). */
    String     portfolioCsvPath;
    LocalDate  asOfDate;

    @Builder.Default VaRMethod    varMethod        = VaRMethod.HISTORICAL;
    @Builder.Default double       confidenceLevel  = 0.99;
    @Builder.Default int          numPaths         = 10_000;
    /**
     * Number of trading-day log-returns to use as the scenario window.
     * The price-load from-date is derived as {@code asOfDate.minusDays(historicalWindow * 2)}.
     */
    @Builder.Default int          historicalWindow = 99;
    @Builder.Default MaturityGrid timeGrid         = MaturityGrid.GRID_53;
}
