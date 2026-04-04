package infrastructure.adapter.in.spark;

import application.port.out.VaRResultPublisher;
import domain.model.MarketData;
import domain.model.Portoflio;
import domain.model.Position;
import domain.model.VaRResult;
import infrastructure.model.EnrichedPositionRow;
import infrastructure.model.VaRResultRow;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.SparkSession;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import workflow.ScenarioNotification;
import workflow.VaRPipeline;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ComposeAdapter {

    private final SparkSession       spark;
    private final VaRPipeline        varPipeline;
    private final VaRResultPublisher varResultPublisher;

    public void compute(Dataset<EnrichedPositionRow> enriched,
                        MarketData marketData,
                        ScenarioNotification notification) {
        log.info("ComposeAdapter: computing VaR for scenario [{}]", notification.getCorrelationId());

        Map<String, List<EnrichedPositionRow>> byPortfolio = enriched.collectAsList()
                .stream()
                .collect(Collectors.groupingBy(EnrichedPositionRow::getPortfolioId));


        List<VaRResultRow> results = new ArrayList<>();
        byPortfolio.forEach((portfolioId, rows) -> {
            Portoflio portfolio = buildPortfolio(portfolioId, rows);
            VaRResult varResult = varPipeline.execute(portfolio, marketData, notification);
            log.info("  Portfolio {} → VaR={}", portfolioId, varResult.getVar());
            varResultPublisher.publish(portfolio, notification.getAsOfDate(),
                    notification.getConfidenceLevel(), varResult.getVar());
            results.add(buildResultRow(
                    notification.getCorrelationId(), portfolioId, notification.getAsOfDate(), varResult));
        });

        // Streaming-style push: each partition writes its rows independently
        spark.createDataset(results, Encoders.bean(VaRResultRow.class))
                .foreachPartition((Iterator<VaRResultRow> iter) ->
                        iter.forEachRemaining(row ->
                                LoggerFactory.getLogger(ComposeAdapter.class)
                                        .info("[Stream] Persist → portfolio={} VaR={} α={}",
                                                row.getPortfolioId(), row.getVarAmount(), row.getAlpha())));

        log.info("ComposeAdapter: scenario [{}] — {}/{} portfolio(s) written",
                notification.getCorrelationId(), results.size(), byPortfolio.size());
    }

    private Portoflio buildPortfolio(String portfolioId, List<EnrichedPositionRow> rows) {
        List<Position> positions = rows.stream()
                .map(r -> Position.equitySpot(r.getTicker(), r.getQuantity(), r.getSpotPrice()))
                .collect(Collectors.toList());
        return Portoflio.builder().id(portfolioId).positions(positions).build();
    }

    private VaRResultRow buildResultRow(String correlationId, String portfolioId,
                                        LocalDate asOfDate, VaRResult result) {
        VaRResultRow row = new VaRResultRow();
        row.setCorrelationId(correlationId);
        row.setPortfolioId(portfolioId);
        row.setAsOfDate(asOfDate.toString());
        row.setVarAmount(result.getVar());
        row.setAlpha(result.getAlpha());
        row.setNumberOfScenarios(result.getNumberOfScenarios());
        row.setMeanPnL(result.getMeanPnL());
        row.setStdDevPnL(result.getStdDevPnL());
        return row;
    }
}
