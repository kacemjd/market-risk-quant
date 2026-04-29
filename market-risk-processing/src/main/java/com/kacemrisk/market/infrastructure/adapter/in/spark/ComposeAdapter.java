package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.Position;
import com.kacemrisk.market.infrastructure.model.RiskPosition;
import com.kacemrisk.market.workflow.RunContext;
import com.kacemrisk.market.workflow.VaROutput;
import com.kacemrisk.market.workflow.VaRPipeline;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

/**
 * Infrastructure adapter that drives the full VaR scenario pipeline.
 *
 * <p>Owns the Spark exit boundary and all orchestration steps:
 * <ol>
 *   <li>Collect {@code Dataset<RiskPosition>} from the join stage</li>
 *   <li>Group positions by {@code portfolioId}</li>
 *   <li>For each group: map to domain types, run {@link VaRPipeline} (calibrate + compute)</li>
 *   <li>Persist calibrated {@code MarketData} via {@link MarketDataRepository}</li>
 *   <li>Fan-out {@code VaRResult} to all registered {@link VaRResultPublisher} sinks</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeAdapter {

    private final VaRPipeline varPipeline;
    private final MarketDataRepository marketDataRepository;
    private final List<VaRResultPublisher> publishers;

    /**
     * Entry point — receives the lazy enriched dataset directly from {@link JoinAdapter}.
     */
    public void compute(Dataset<RiskPosition> positionsDs, RunContext ctx) {
        log.info("[ComposeAdapter] Scenario [{}] — collecting positions", ctx.correlationId());

        List<RiskPosition> positions = positionsDs.collectAsList();  // ← single Spark exit boundary
        positionsDs.unpersist();

        if (positions.isEmpty()) {
            log.error("[ComposeAdapter] No positions after enrichment — aborting scenario [{}]", ctx.correlationId());
            return;
        }

        Map<String, List<RiskPosition>> byPortfolio = positions.stream()
                .collect(groupingBy(RiskPosition::getPortfolioId));

        log.info("[ComposeAdapter] Scenario [{}] — {} portfolio(s) to process",
                ctx.correlationId(), byPortfolio.size());

        byPortfolio.forEach((portfolioId, group) -> {
            Portfolio portfolio = buildPortfolio(portfolioId, group);

            Map<String, double[]> pricesByTicker = group.stream()
                    .collect(toMap(RiskPosition::getTicker, RiskPosition::getPriceHistory, (a, b) -> a));

            VaROutput output = varPipeline.execute(portfolio, pricesByTicker, ctx);

            marketDataRepository.save(output.marketData());
            log.info("[ComposeAdapter] portfolio={} | VaR={} | ES={} | tickers={}",
                    portfolioId, output.varResult().getVar(),
                    output.varResult().getExpectedShortfall(), pricesByTicker.keySet());

            publishers.forEach(sink -> sink.publish(
                    ctx.correlationId(), output.portfolio(),
                    ctx.asOfDate(), output.varResult(), ctx.varMethod()));
        });

        log.info("[ComposeAdapter] Scenario [{}] complete — {} portfolio(s) published",
                ctx.correlationId(), byPortfolio.size());
    }

    // ── RiskPosition → domain Portfolio ──────────────────────────────────────

    private Portfolio buildPortfolio(String portfolioId, List<RiskPosition> group) {
        List<Position> positions = group.stream()
                .map(this::toPosition)
                .collect(Collectors.toList());
        return Portfolio.builder()
                .id(portfolioId)
                .positions(positions)
                .build();
    }

    /**
     * Maps one {@link RiskPosition} to a domain {@link Position}.
     *
     * <p>Uses the dedicated factory for known linear instruments.
     * Non-equity asset classes (CTY, FX, IRD) default to delta=1.0 / gamma=0.0 / maturity=0.0
     * (linear spot assumption) — extend this switch when options or futures are introduced.
     */
    private Position toPosition(RiskPosition r) {
        return switch (r.getAssetClass()) {
            case EQD -> Position.equitySpot(r.getTicker(), r.getQuantity(), r.getSpotPrice());
            case CTY, FX, IRD -> Position.builder()
                    .ticker(r.getTicker())
                    .assetClass(r.getAssetClass())
                    .quantity(r.getQuantity())
                    .spotPrice(r.getSpotPrice())
                    .delta(1.0)
                    .gamma(0.0)
                    .maturityInYears(0.0)
                    .build();
        };
    }
}
