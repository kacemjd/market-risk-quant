package com.kacemrisk.market.domain.exception;

import com.kacemrisk.market.domain.model.AssetClass;
import lombok.Getter;

/**
 * Raised when a historical price fetch fails for a specific instrument.
 *
 * <p>Carries enough context for structured audit logging — ticker symbol and asset class — so that
 * the bulk-fetch orchestrator can skip the position and continue processing the rest of the
 * portfolio without losing traceability.
 */
@Getter
public class HistoricalPriceFetchException extends DomainException {

    /** Instrument whose price could not be retrieved. */
    private final String ticker;

    /** Asset class of the failing instrument (drives strategy selection). */
    private final AssetClass assetClass;

    public HistoricalPriceFetchException(String ticker, AssetClass assetClass, String message) {
        super(message);
        this.ticker = ticker;
        this.assetClass = assetClass;
    }

    public HistoricalPriceFetchException(
            String ticker, AssetClass assetClass, String message, Throwable cause) {
        super(message, cause);
        this.ticker = ticker;
        this.assetClass = assetClass;
    }
}
