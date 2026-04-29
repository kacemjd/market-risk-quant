package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.application.port.in.CalculateVaRCommand;
import com.kacemrisk.market.application.port.in.CalculateVaRUseCase;
import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.PortfolioFactory;
import com.kacemrisk.market.domain.model.VaRResult;
import com.kacemrisk.market.infrastructure.model.RiskPositionSlim;
import com.kacemrisk.market.workflow.PortfolioVaRResult;
import com.kacemrisk.market.workflow.RunContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.MapFunction;
import org.apache.spark.api.java.function.MapGroupsFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Encoders;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Infrastructure adapter that drives the full VaR scenario pipeline.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li><b>Calibrate</b> {@link MarketData} once per scenario from the price broadcast on the driver —
 *       avoids redundant O(tickers²) covariance recomputation per portfolio</li>
 *   <li><b>Release</b> the price broadcast immediately after calibration, before VaR executors start —
 *       prevents two large broadcasts co-existing in executor memory simultaneously</li>
 *   <li><b>Persist</b> calibrated {@link MarketData} once per scenario via a single batch write</li>
 *   <li><b>Distribute VaR</b> across Spark executors via {@code groupByKey(portfolioId).mapGroups} —
 *       only compact {@link PortfolioVaRResult} objects are collected back to the driver</li>
 *   <li><b>Fan-out results</b> to all registered {@link VaRResultPublisher}s</li>
 * </ol>
 *
 * <h3>Broadcast lifecycle</h3>
 * <pre>
 *   priceBroadcast  ──► calibration (driver) ──► release()  ──► destroyed
 *   mdBroadcast     ──────────────────────────► mapGroups (executors) ──► destroy(false)
 *   ctxBroadcast    ──────────────────────────► mapGroups (executors) ──► destroy(false)
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeAdapter {

    private final JavaSparkContext jsc;
    private final CalibrateMarketDataUseCase calibrateMarketData;
    private final MarketDataRepository marketDataRepository;
    private final CalculateVaRUseCase calculateVaRUseCase;
    private final List<VaRResultPublisher> publishers;

    /**
     * Entry point — receives the enriched dataset from {@link JoinAdapter} and drives
     * the full scenario pipeline to completion.
     */
    public void compute(EnrichedDataset enriched, RunContext ctx) {
        if (enriched.positions() == null || enriched.priceBroadcast() == null) {
            log.error("[ComposeAdapter] Received empty enriched dataset for scenario [{}] — aborting",
                    ctx.correlationId());
            return;
        }

        log.info("[ComposeAdapter] Scenario [{}] — calibrating market data", ctx.correlationId());

        // Calibrate once per scenario from the price broadcast — local driver read, no RPC.
        Map<String, double[]> allPrices = enriched.priceBroadcast().value();

        MarketData marketData = calibrateMarketData.calibrate(ctx.asOfDate(), allPrices);
        log.info("[ComposeAdapter] Scenario [{}] — calibrated {} risk factor(s)",
                ctx.correlationId(), marketData.getRiskFactors().size());

        // Persist calibrated MarketData ONCE per scenario (batch insert).
        marketDataRepository.save(marketData);

        // Release price broadcast BEFORE launching executor VaR work
        // Freeing executor broadcast memory now prevents two large broadcasts co-existing.
        enriched.release();

        // Broadcast compact MarketData + RunContext to executors
        // Both types now implement Serializable.
        Broadcast<MarketData> mdBroadcast  = jsc.broadcast(marketData);
        Broadcast<RunContext> ctxBroadcast = jsc.broadcast(ctx);

        try {
            // VaR is computed on executors — each executor group receives the positions for one
            // portfolioId and returns a compact result object. Only those result objects (~100 bytes
            // each) are collected to the driver, not the full position dataset.
            List<PortfolioVaRResult> results = enriched.positions()
                    .groupByKey((MapFunction<RiskPositionSlim, String>) RiskPositionSlim::portfolioId, Encoders.STRING())
                    .mapGroups(
                            (MapGroupsFunction<String, RiskPositionSlim, PortfolioVaRResult>)
                                    (portfolioId, posIter) -> {
                                        List<RiskPositionSlim> group = new ArrayList<>();
                                        posIter.forEachRemaining(group::add);

                                        Portfolio portfolio = PortfolioFactory.build(portfolioId, group);
                                        VaRResult varResult = calculateVaROnExecutor(
                                                portfolio, mdBroadcast.value(), ctxBroadcast.value());
                                        return new PortfolioVaRResult(portfolioId, portfolio, varResult);
                                    },
                            Encoders.kryo(PortfolioVaRResult.class))
                    .collectAsList();

            log.info("[ComposeAdapter] Scenario [{}] — {} portfolio(s) computed",
                    ctx.correlationId(), results.size());

            // Fan-out results on the driver (tiny result objects)
            publishers.forEach(sink ->
                    results.forEach(r -> sink.publish(
                            ctx.correlationId(), r.portfolio(),
                            ctx.asOfDate(), r.varResult(), ctx.varMethod())));

            log.info("[ComposeAdapter] Scenario [{}] complete — {} portfolio(s) published",
                    ctx.correlationId(), results.size());

        } finally {
            mdBroadcast.destroy(false);
            ctxBroadcast.destroy(false);
        }
    }

    /**
     * Executes the VaR calculation for a single portfolio inside a Spark executor closure.
     * Must not reference Spring-managed beans — {@link CalculateVaRUseCase} is wired as a
     * framework-free domain service and is safe to call from executor context.
     */
    private VaRResult calculateVaROnExecutor(Portfolio portfolio, MarketData marketData,
                                              RunContext ctx) {
        return calculateVaRUseCase.calculate(CalculateVaRCommand.builder()
                .portfolio(portfolio)
                .marketData(marketData)
                .method(ctx.varMethod())
                .alpha(ctx.confidenceLevel())
                .numPaths(ctx.numPaths())
                .historicalWindow(ctx.historicalWindow())
                .timeGrid(ctx.timeGrid())
                .build());
    }
}
