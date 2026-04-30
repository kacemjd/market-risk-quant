package com.kacemrisk.market.infrastructure.model;

import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.PortfolioFactory;
import java.io.Serial;
import java.io.Serializable;

/**
 * Slim enriched position — carries only the fields needed for VaR computation on executors.
 *
 * <p>Does not carry {@code priceHistory}. Historical prices are used for calibration on the driver
 * via the price broadcast and do not need to travel through the {@code groupByKey(portfolioId)}
 * shuffle. Executors receive pre-calibrated {@link com.kacemrisk.market.domain.model.MarketData}
 * via a separate broadcast — no per-position price data is needed at VaR time.
 *
 * <p>Implements {@link PortfolioFactory.RiskPositionView} so that {@link
 * com.kacemrisk.market.domain.model.PortfolioFactory#build} can be called directly from executor
 * closures without any change to the domain factory.
 */
public record RiskPositionSlim(
        String portfolioId, String ticker, double quantity, AssetClass assetClass, double spotPrice)
        implements Serializable, PortfolioFactory.RiskPositionView {

    @Serial private static final long serialVersionUID = 1L;

    // ── PortfolioFactory.RiskPositionView contract ────────────────────────────────────────────

    @Override
    public String getTicker() {
        return ticker;
    }

    @Override
    public AssetClass getAssetClass() {
        return assetClass;
    }

    @Override
    public double getQuantity() {
        return quantity;
    }

    @Override
    public double getSpotPrice() {
        return spotPrice;
    }
}
