package com.kacemrisk.market.workflow;

import com.kacemrisk.market.application.port.in.CalculateVaRCommand;
import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Framework-free VaR pipeline.
 *
 * <p>Owns the full per-portfolio computation chain:
 * <ol>
 *   <li>Calibrate market data (volatilities, covariance) from price histories</li>
 *   <li>Compute VaR using the configured strategy</li>
 * </ol>
 *
 * <p>No Spring annotations — instantiated and wired by infrastructure configuration.
 */
@Slf4j
@RequiredArgsConstructor
public class VaRCalculationPipeline implements VaRPipeline {

    private final CalibrateMarketDataUseCase calibrateMarketData;
    private final CalculateVaRUseCase calculateVaR;

    @Override
    public VaROutput execute(Portfolio portfolio, Map<String, double[]> pricesByTicker, RunContext ctx) {
        log.debug("[Pipeline] portfolio={} | tickers={} | method={} | α={}",
                portfolio.getId(), pricesByTicker.keySet(), ctx.varMethod(), ctx.confidenceLevel());

        // Step 1 — calibrate market data for this portfolio's tickers
        MarketData marketData = calibrateMarketData.calibrate(ctx.asOfDate(), pricesByTicker);
        log.debug("[Pipeline] Calibrated {} risk factor(s) for portfolio={}",
                marketData.getRiskFactors().size(), portfolio.getId());

        // Step 2 — compute VaR
        VaRResult varResult = calculateVaR.calculate(CalculateVaRCommand.builder()
                .portfolio(portfolio)
                .marketData(marketData)
                .method(ctx.varMethod())
                .alpha(ctx.confidenceLevel())
                .numPaths(ctx.numPaths())
                .historicalWindow(ctx.historicalWindow())
                .timeGrid(ctx.timeGrid())
                .build());

        log.debug("[Pipeline] portfolio={} | VaR={} | ES={}",
                portfolio.getId(), varResult.getVar(), varResult.getExpectedShortfall());

        return new VaROutput(portfolio, marketData, varResult);
    }
}
