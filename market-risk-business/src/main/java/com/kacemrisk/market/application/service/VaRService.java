package com.kacemrisk.market.application.service;

import com.kacemrisk.market.application.port.in.CalculateVaRCommand;
import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.domain.model.VaRResult;
import com.kacemrisk.market.domain.service.simulation.VaRCalculator;
import com.kacemrisk.market.domain.service.simulation.VaRCalculatorFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * Application service that orchestrates the VaR calculation.
 *
 * <p>It maps the inputs from the driving port into a configured domain strategy
 * and delegates the core computation to it.
 */
@Slf4j
public class VaRService implements CalculateVaRUseCase {

    /**
     * {@inheritDoc}
     *
     * <p>Builds the appropriate {@link VaRCalculator} from the factory
     * and executes the calculation.
     */
    @Override
    public VaRResult calculate(CalculateVaRCommand command) {
        log.info("VaR Request | strategy={} | portfolio={} | asOfDate={} | α={}",
                command.getMethod(),
                command.getPortfolio().getId(),
                command.getMarketData().getAsOfDate(),
                command.getAlpha());

        // Delegate to Domain factory to construct the appropriate calculator
        VaRCalculator calculator = VaRCalculatorFactory.create(
                command.getMethod(),
                command.getNumPaths(),
                command.getTimeGrid(),
                command.getHistoricalWindow()
        );

        // Execute Domain rule
        VaRResult result = calculator.calculate(
                command.getPortfolio(),
                command.getMarketData(),
                command.getAlpha()
        );

        log.info("VaR complete | strategy={} | VaR={} | ES={}",
                calculator.getClass().getSimpleName(),
                result.getVar(),
                result.getExpectedShortfall());

        return result;
    }
}



