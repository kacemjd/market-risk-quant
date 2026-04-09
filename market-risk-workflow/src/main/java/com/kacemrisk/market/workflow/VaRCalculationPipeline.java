package com.kacemrisk.market.workflow;

import com.kacemrisk.market.application.port.in.CalculateVaRCommand;
import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;
import lombok.RequiredArgsConstructor;

/**
 * Method-agnostic VaR pipeline.
 *
 * <p>This acts as a Driving Adapter that maps incoming triggers into the
 * standardized {@link CalculateVaRCommand} and forwards it to the application port.
 */
@RequiredArgsConstructor
public class VaRCalculationPipeline implements VaRPipeline {

    private final CalculateVaRUseCase calculateVaRUseCase;

    @Override
    public VaRResult execute(Portfolio portfolio, MarketData marketData, ScenarioNotification notification) {
        CalculateVaRCommand command = CalculateVaRCommand.builder()
                .portfolio(portfolio)
                .marketData(marketData)
                .method(notification.getVarMethod())
                .alpha(notification.getConfidenceLevel())
                .numPaths(notification.getNumPaths())
                .historicalWindow(notification.getHistoricalWindow())
                .timeGrid(notification.getTimeGrid())
                .build();

        return calculateVaRUseCase.calculate(command);
    }
}




