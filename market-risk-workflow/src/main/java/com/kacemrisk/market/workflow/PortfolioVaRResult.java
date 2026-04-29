package com.kacemrisk.market.workflow;

import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;

import java.io.Serial;
import java.io.Serializable;

/**
 * Spark-serialisable result carrier for one portfolio's VaR computation.
 *
 * <p>Returned from the {@code mapGroups} closure in {@code ComposeAdapter} and collected
 * to the driver as {@code List<PortfolioVaRResult>}. Only these compact result objects
 * travel to the driver — not the full enriched positions dataset.
 */
public record PortfolioVaRResult(
        String portfolioId,
        Portfolio portfolio,
        VaRResult varResult
) implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
}
