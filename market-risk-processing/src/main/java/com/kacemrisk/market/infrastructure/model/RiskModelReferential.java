package com.kacemrisk.market.infrastructure.model;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The pure data universe the risk engine operates on.
 *
 * <p>Contains one {@link RiskPosition} per deduplicated (portfolioId, ticker) pair.
 * No execution parameters here — those belong in
 * {@link com.kacemrisk.market.workflow.RunContext}.
 */
public record RiskModelReferential(List<RiskPosition> positions) {

    public Map<String, List<RiskPosition>> positionsByPortfolio() {
        return positions.stream()
                .collect(Collectors.groupingBy(RiskPosition::getPortfolioId));
    }
}

