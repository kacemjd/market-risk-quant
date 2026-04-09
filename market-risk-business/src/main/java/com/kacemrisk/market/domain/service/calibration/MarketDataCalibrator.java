package com.kacemrisk.market.domain.service.calibration;

import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.domain.model.MarketData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class MarketDataCalibrator implements CalibrateMarketDataUseCase {

    private final MarketDataCalibrationService calibrationService;

    @Override
    public MarketData calibrate(LocalDate asOfDate, Map<String, List<Double>> historicalPrices) {
        log.info("MarketDataCalibrator — starting calibration for asOfDate={}", asOfDate);
        return calibrationService.calibrateFromPrices(asOfDate, historicalPrices);
    }
}
