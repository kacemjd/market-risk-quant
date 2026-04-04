package application.port.in;

import domain.model.MarketData;

import java.time.LocalDate;

/**
 * Driving port — entry point for triggering a market data calibration.
 */
public interface CalibrateMarketDataUseCase {

    /**
     * @param asOfDate reference date for the calibration window
     * @return a fully populated {@link MarketData} object ready for VaR calculation
     */
    MarketData calibrate(LocalDate asOfDate);
}

