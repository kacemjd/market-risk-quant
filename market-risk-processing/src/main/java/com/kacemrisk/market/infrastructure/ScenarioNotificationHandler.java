package com.kacemrisk.market.infrastructure;

import com.kacemrisk.market.infrastructure.adapter.in.spark.ComposeAdapter;
import com.kacemrisk.market.infrastructure.adapter.in.spark.JoinAdapter;
import com.kacemrisk.market.infrastructure.adapter.in.spark.SparkMarketDataIngestionAdapter;
import com.kacemrisk.market.infrastructure.model.EnrichedPositionRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.springframework.stereotype.Component;
import com.kacemrisk.market.domain.model.MarketData;
import com.kacemrisk.market.workflow.ScenarioNotification;
import com.kacemrisk.market.workflow.TriggerScenarioUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioNotificationHandler implements TriggerScenarioUseCase {

    private final SparkMarketDataIngestionAdapter ingestionAdapter;
    private final JoinAdapter joinAdapter;
    private final ComposeAdapter composeAdapter;

    @Override
    public String trigger(ScenarioNotification notification) {

        log.info(">>> Scenario [{}] started | asOfDate={} | paths={} | α={}",
                notification.getCorrelationId(),
                notification.getAsOfDate(),
                notification.getNumPaths(),
                notification.getConfidenceLevel());

        MarketData marketData = ingestionAdapter.ingestDirectory(
                notification.getPricesCsvPath(), notification.getAsOfDate());

        Dataset<EnrichedPositionRow> enriched = joinAdapter.enrich(notification);

        composeAdapter.compute(enriched, marketData, notification);

        log.info("<<< Scenario [{}] completed", notification.getCorrelationId());
        return notification.getCorrelationId();
    }
}

