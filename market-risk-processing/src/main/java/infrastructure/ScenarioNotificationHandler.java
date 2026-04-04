package infrastructure;

import infrastructure.adapter.in.spark.ComposeAdapter;
import infrastructure.adapter.in.spark.JoinAdapter;
import infrastructure.adapter.in.spark.SparkMarketDataIngestionAdapter;
import infrastructure.model.EnrichedPositionRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.springframework.stereotype.Component;
import domain.model.MarketData;
import workflow.ScenarioNotification;
import workflow.TriggerScenarioUseCase;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScenarioNotificationHandler implements TriggerScenarioUseCase {

    private final SparkMarketDataIngestionAdapter ingestionAdapter;
    private final JoinAdapter                     joinAdapter;
    private final ComposeAdapter                  composeAdapter;

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

