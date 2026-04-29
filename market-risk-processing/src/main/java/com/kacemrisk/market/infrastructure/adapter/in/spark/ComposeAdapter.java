package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.PortfolioFactory;
import com.kacemrisk.market.infrastructure.model.RiskPosition;
import com.kacemrisk.market.workflow.RunContext;
import com.kacemrisk.market.workflow.VaROutput;
import com.kacemrisk.market.workflow.VaRPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Infrastructure adapter that drives the full VaR scenario pipeline.
 *
 * <h3>Responsibilities</h3>
 * <ol>
 *   <li>Collect {@link EnrichedDataset} positions from the Spark join stage and
 *       release all Spark-managed resources (broadcast + cached portfolio dataset)</li>
 *   <li>Calibrate {@link MarketData} <b>once per scenario</b> from the full price universe —
 *       avoids N × O(tickers²) redundant calibrations (Phase 2, P2.5)</li>
 *   <li>Persist calibrated {@link MarketData} <b>once per scenario</b> via
 *       {@link MarketDataRepository} — eliminates N duplicate writes (Phase 2, P2.5)</li>
 *   <li>For each portfolio group: build domain {@link Portfolio} via {@link PortfolioFactory},
 *       run {@link VaRPipeline} (VaR only — calibration already done), fan-out results</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeAdapter {

    private final VaRPipeline varPipeline;
    private final CalibrateMarketDataUseCase calibrateMarketData;
    private final MarketDataRepository marketDataRepository;
    private final List<VaRResultPublisher> publishers;

    /**
     * Entry point — receives the enriched dataset from {@link JoinAdapter}.
     *
     * <p>Spark resources carried by {@code enriched} are released in a {@code finally}
     * block immediately after {@code collectAsList()} — see {@link EnrichedDataset#release()}.
     */
    public void compute(EnrichedDataset enriched, RunContext ctx) {
        if (enriched.positions() == null || enriched.priceBroadcast() == null) {
            log.error("[ComposeAdapter] Received empty enriched dataset for scenario [{}] — aborting",
                    ctx.correlationId());
            return;
        }

        log.info("[ComposeAdapter] Scenario [{}] — collecting positions", ctx.correlationId());

        List<RiskPosition> positions;
        try {
            positions = enriched.positions().collectAsList();  // ← single Spark exit boundary
        } finally {
            enriched.release();  // destroy broadcast + unpersist portfolioDs (C2+C3 fixes)
        }

        if (positions.isEmpty()) {
            log.error("[ComposeAdapter] No positions after enrichment — aborting scenario [{}]",
                    ctx.correlationId());
            return;
        }

        // ── Calibrate ONCE per scenario from the full price universe ──────────────────────
        Map<String, double[]> allPrices = positions.stream()
                .filter(p -> p.getPriceHistory() != null && p.getPriceHistory().length > 0)
                .collect(toMap(RiskPosition::getTicker, RiskPosition::getPriceHistory, (a, b) -> a));

        MarketData marketData = calibrateMarketData.calibrate(ctx.asOfDate(), allPrices);
        log.info("[ComposeAdapter] Scenario [{}] — calibrated {} risk factor(s)",
                ctx.correlationId(), marketData.getRiskFactors().size());

        // ── Persist calibrated MarketData ONCE per scenario ───────────────────────────────
        marketDataRepository.save(marketData);

        // ── Group by portfolio and compute VaR for each ───────────────────────────────────
        Map<String, List<RiskPosition>> byPortfolio = positions.stream()
                .collect(groupingBy(RiskPosition::getPortfolioId));

        log.info("[ComposeAdapter] Scenario [{}] — {} portfolio(s) to process",
                ctx.correlationId(), byPortfolio.size());

        byPortfolio.forEach((portfolioId, group) -> {
            Portfolio portfolio = PortfolioFactory.build(portfolioId, group);

            VaROutput output = varPipeline.execute(portfolio, marketData, ctx);

            log.info("[ComposeAdapter] portfolio={} | VaR={} | ES={} | tickers={}",
                    portfolioId, output.varResult().getVar(),
                    output.varResult().getExpectedShortfall(),
                    group.stream().map(RiskPosition::getTicker).collect(Collectors.toSet()));

            publishers.forEach(sink -> sink.publish(
                    ctx.correlationId(), output.portfolio(),
                    ctx.asOfDate(), output.varResult(), ctx.varMethod()));
        });

        log.info("[ComposeAdapter] Scenario [{}] complete — {} portfolio(s) published",
                ctx.correlationId(), byPortfolio.size());
    }
}
