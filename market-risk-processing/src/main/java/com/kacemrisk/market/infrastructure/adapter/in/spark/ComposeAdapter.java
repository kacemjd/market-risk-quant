package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.application.port.out.VaRResultPublisher;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.domain.model.Portfolio;
import com.kacemrisk.market.domain.model.Position;
import com.kacemrisk.market.domain.model.VaRResult;
import com.kacemrisk.market.infrastructure.model.RiskPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.kacemrisk.market.workflow.RunContext;
import com.kacemrisk.market.workflow.VaRPipeline;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Drives the VaR business pipeline for every portfolio in the referential and fans out
 * results to the configured sinks.
 *
 * <p>Sinks are implementations of {@link VaRResultPublisher}, selected at startup via
 * {@code output.sink}:
 * <ul>
 *   <li>{@code log}     → {@link com.kacemrisk.market.infrastructure.adapter.out.publisher.LoggingVaRResultPublisher} (default)</li>
 *   <li>{@code questdb} → {@link com.kacemrisk.market.infrastructure.adapter.out.publisher.QuestDbVaRResultPublisher}</li>
 *   <li>{@code kafka}   → {@link com.kacemrisk.market.infrastructure.adapter.out.publisher.KafkaVaRResultPublisher}</li>
 * </ul>
 * To add a Parquet or CSV sink, implement {@link VaRResultPublisher} and register it as a
 * Spring bean — no changes here required.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeAdapter {

    private final VaRPipeline varPipeline;
    private final List<VaRResultPublisher> publishers;

    /**
     * Iterates over every portfolio group, executes the VaR pipeline, and fans the result
     * out to all registered sinks.
     *
     * @param positionsByPortfolio positions pre-grouped by portfolioId
     *                             (from {@link com.kacemrisk.market.infrastructure.model.RiskModelReferential#positionsByPortfolio()})
     * @param marketData           calibrated volatilities and covariance
     * @param ctx                  execution metadata (method, confidence level, etc.)
     */
    public void compute(Map<String, List<RiskPosition>> positionsByPortfolio,
                        MarketData marketData,
                        RunContext ctx) {
        log.info("ComposeAdapter: computing VaR for scenario [{}] — {} portfolio(s)",
                ctx.correlationId(), positionsByPortfolio.size());

        positionsByPortfolio.forEach((portfolioId, rows) -> {
            Portfolio portfolio = buildPortfolio(portfolioId, rows);
            VaRResult varResult = varPipeline.execute(portfolio, marketData, ctx);

            log.info("  portfolio={} | VaR={} | ES={}",
                    portfolioId, varResult.getVar(), varResult.getExpectedShortfall());

            // Fan out to all registered sinks (log / QuestDB / Kafka / …)
            publishers.forEach(sink -> sink.publish(
                    ctx.correlationId(), portfolio,
                    ctx.asOfDate(), varResult, ctx.varMethod()));
        });

        log.info("ComposeAdapter: scenario [{}] complete — {} portfolio(s) published",
                ctx.correlationId(), positionsByPortfolio.size());
    }

    // ── Portfolio assembly ────────────────────────────────────────────────────────────────

    private Portfolio buildPortfolio(String portfolioId, List<RiskPosition> rows) {
        List<Position> positions = rows.stream()
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
     * <p>Spot (linear) instrument assumptions:
     * <ul>
     *   <li>delta = 1.0 — P&amp;L moves 1:1 with the spot price</li>
     *   <li>gamma = 0.0 — no convexity for spot positions</li>
     *   <li>maturityInYears = 0.0 — no time dimension for spot</li>
     * </ul>
     * When options or futures are introduced, extend this method to branch on
     * {@code riskPosition.getAssetClass()} and apply the appropriate pricing factory.
     */
    private Position toPosition(RiskPosition r) {
        return Position.builder()
                .ticker(r.getTicker())
                .assetClass(r.getAssetClass())   // ← actual enum from RiskPosition, not hardcoded EQD
                .quantity(r.getQuantity())
                .spotPrice(r.getSpotPrice())
                .delta(1.0)
                .gamma(0.0)
                .maturityInYears(0.0)
                .build();
    }
}
