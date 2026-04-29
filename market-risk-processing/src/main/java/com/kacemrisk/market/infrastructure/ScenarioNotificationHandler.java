package com.kacemrisk.market.infrastructure;

import com.kacemrisk.market.application.port.in.CalibrateMarketDataUseCase;
import com.kacemrisk.market.application.port.out.MarketDataRepository;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.infrastructure.adapter.in.spark.ComposeAdapter;
import com.kacemrisk.market.infrastructure.adapter.in.spark.JoinAdapter;
import com.kacemrisk.market.infrastructure.model.RiskModelReferential;
import com.kacemrisk.market.infrastructure.model.RiskPosition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.kacemrisk.market.workflow.RunContext;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;

import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Primary implementation of {@link TriggerScenarioUseCase}.
 *
 * <p>Pure orchestrator — delegates all data loading to {@link JoinAdapter} and
 * all VaR computation to {@link ComposeAdapter}. No raw data access happens here.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioNotificationHandler implements TriggerScenarioUseCase {

    private final JoinAdapter joinAdapter;
    private final CalibrateMarketDataUseCase calibrateMarketData;
    private final MarketDataRepository marketDataRepository;
    private final ComposeAdapter composeAdapter;

    @Override
    public String trigger(ScenarioNotification notification) {
        log.info(">>> Scenario [{}] | asOfDate={} | method={} | window={} | α={}",
                notification.getCorrelationId(), notification.getAsOfDate(),
                notification.getVarMethod(), notification.getHistoricalWindow(),
                notification.getConfidenceLevel());

        RunContext ctx = RunContext.from(notification);
        RiskModelReferential ref = joinAdapter.enrich(ctx);

        Map<String, double[]> pricesByTicker = ref.positions().stream()
                .collect(toMap(RiskPosition::getTicker, RiskPosition::getPriceHistory, (a, b) -> a));

        MarketData marketData = calibrateMarketData.calibrate(ctx.asOfDate(), pricesByTicker);
        marketDataRepository.save(marketData);
        log.info("Market data calibrated and persisted | tickers={}", marketData.getRiskFactors().size());

        composeAdapter.compute(ref.positionsByPortfolio(), marketData, ctx);

        log.info("<<< Scenario [{}] completed", ctx.correlationId());
        return ctx.correlationId();
    }
}
