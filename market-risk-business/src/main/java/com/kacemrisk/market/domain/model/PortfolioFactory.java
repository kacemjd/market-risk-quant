package com.kacemrisk.market.domain.model;

import java.util.List;

/**
 * Domain factory — builds a {@link Portfolio} from a group of enriched positions.
 *
 * <p>Centralises the {@code RiskPosition → Position → Portfolio} mapping so that
 * infrastructure adapters ({@code ComposeAdapter}) don't carry domain switch logic.
 *
 * <p>The {@link AssetClass} switch encodes per-class pricing assumptions:
 * <ul>
 *   <li>{@code EQD} — equity spot: delta=1.0, gamma=0.0, maturity=0.0 (delegates to
 *       {@link Position#equitySpot})</li>
 *   <li>{@code CTY, FX, IRD} — linear spot assumption: delta=1.0, gamma=0.0, maturity=0.0.
 *       Extend this switch when options or futures are introduced.</li>
 * </ul>
 *
 * <p>This factory is framework-free and has no Spring dependencies — safe to call from
 * Spark executor closures once Phase 2C (VaR-on-executors) lands.
 *
 * @param <R> a type that exposes the fields required to build a {@link Position}
 *            (ticker, assetClass, quantity, spotPrice)
 */
public final class PortfolioFactory {

    private PortfolioFactory() {}

    /**
     * Builds a {@link Portfolio} from a group of {@link RiskPositionView} entries that all
     * share the same {@code portfolioId}.
     *
     * @param portfolioId the portfolio identifier
     * @param group       positions belonging to this portfolio
     * @return fully constructed {@link Portfolio}
     */
    public static Portfolio build(String portfolioId, List<? extends RiskPositionView> group) {
        List<Position> positions = group.stream()
                .map(PortfolioFactory::toPosition)
                .toList();
        return Portfolio.builder()
                .id(portfolioId)
                .positions(positions)
                .build();
    }

    /**
     * Maps one enriched position view to a domain {@link Position}.
     *
     * <p>Non-equity asset classes (CTY, FX, IRD) default to delta=1.0 / gamma=0.0 /
     * maturity=0.0 (linear spot assumption). Extend this switch when options or
     * futures are introduced.
     */
    public static Position toPosition(RiskPositionView r) {
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

    /**
     * Minimal view of an enriched position required by this factory.
     *
     * <p>Implemented by {@code RiskPosition} (infrastructure model) so the factory
     * can be used in adapters without importing infrastructure types.
     */
    public interface RiskPositionView {
        String getTicker();
        AssetClass getAssetClass();
        double getQuantity();
        double getSpotPrice();
    }
}

