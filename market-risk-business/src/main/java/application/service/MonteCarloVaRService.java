package application.service;

import application.port.in.RunMonteCarloVaRUseCase;
import domain.model.MarketData;
import domain.model.MaturityGrid;
import domain.model.Portoflio;
import domain.model.VaRResult;
import domain.service.calibration.MatrixCalibrator;
import domain.service.simulation.MonteCarloSimulator;
import domain.service.simulation.VaRAggregator;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Builder
public class MonteCarloVaRService implements RunMonteCarloVaRUseCase {

    @Builder.Default private final int          numPaths        = 10_000;
    @Builder.Default private final double       confidenceLevel = 0.99;
    @Builder.Default private final MaturityGrid timeGrid        = MaturityGrid.GRID_53;
    @Builder.Default private final long         seed            = 42L;

    @Override
    public VaRResult runSimulation(@NonNull Portoflio portfolio,
                                   @NonNull MarketData marketData) {

        log.info("Starting Monte Carlo VaR | portfolio={} | asOfDate={} | paths={} | α={}",
                portfolio.getId(), marketData.getAsOfDate(), numPaths, confidenceLevel);

        var calibrator = MatrixCalibrator.from(marketData);

        double[][] sigma  = calibrator.calculateSigma();
        double[]   deltas = calibrator.extractDeltas(portfolio);

        double[] scenarios = MonteCarloSimulator.builder()
                .withMatrix(sigma)
                .withDeltas(deltas)
                .withSteps(numPaths)
                .withTimeGrid(timeGrid)
                .withSeed(seed)
                .build()
                .generatePaths();

        VaRResult result = VaRAggregator.of(scenarios)
                .atConfidence(confidenceLevel)
                .compute();

        log.info("Monte Carlo VaR complete | VaR={}", result.getVar());
        return result;
    }
}

