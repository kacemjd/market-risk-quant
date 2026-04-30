package com.kacemrisk.market.workflow;

import com.kacemrisk.market.application.port.in.CalculateVaRCommand;
import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.VaRResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Framework-free VaR pipeline.
 *
 * <p>Owns the per-portfolio VaR computation step only. Calibration has been moved to scenario level
 * ({@code ComposeAdapter}) so the same {@link MarketData} is reused across all portfolio groups —
 * eliminating redundant O(tickers²) covariance recomputation and duplicate {@code MarketData}
 * persistence (Phase 2, P2.5).
 *
 * <p>No Spring annotations — instantiated and wired by infrastructure configuration.
 */
@Slf4j
@RequiredArgsConstructor
public class VaRCalculationPipeline implements VaRPipeline {

    private final CalculateVaRUseCase calculateVaR;

    @Override
    public VaROutput execute(Portfolio portfolio, MarketData marketData, RunContext ctx) {
        log.debug(
                "[Pipeline] portfolio={} | method={} | α={}",
                portfolio.getId(),
                ctx.varMethod(),
                ctx.confidenceLevel());

        VaRResult varResult =
                calculateVaR.calculate(
                        CalculateVaRCommand.builder()
                                .portfolio(portfolio)
                                .marketData(marketData)
                                .method(ctx.varMethod())
                                .alpha(ctx.confidenceLevel())
                                .numPaths(ctx.numPaths())
                                .historicalWindow(ctx.historicalWindow())
                                .timeGrid(ctx.timeGrid())
                                .build());

        log.debug(
                "[Pipeline] portfolio={} | VaR={} | ES={}",
                portfolio.getId(),
                varResult.getVar(),
                varResult.getExpectedShortfall());

        return new VaROutput(portfolio, marketData, varResult);
    }
}
