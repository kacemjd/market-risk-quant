package domain.service.calibration;

import application.port.in.CalibrateMarketDataUseCase;
import domain.model.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Domain implementation of {@link CalibrateMarketDataUseCase}.
 *
 * <p>Delegates all mathematical work to {@link MarketDataCalibrationService}.
 * Historical prices are supplied by the caller (e.g. an infrastructure adapter
 * that reads them from a database or file).
 */
@Slf4j
@RequiredArgsConstructor
public class MarketDataCalibrator implements CalibrateMarketDataUseCase {

    private final MarketDataCalibrationService calibrationService;
    private final Map<String, List<Double>> historicalPrices;

    /**
     * {@inheritDoc}
     *
     * <p>Passes {@code asOfDate} and the injected historical prices to the
     * {@link MarketDataCalibrationService} and returns the resulting snapshot.
     */
    @Override
    public MarketData calibrate(LocalDate asOfDate) {
        log.info("MarketDataCalibrator — starting calibration for asOfDate={}", asOfDate);
        return calibrationService.calibrateFromPrices(asOfDate, historicalPrices);
    }
}
