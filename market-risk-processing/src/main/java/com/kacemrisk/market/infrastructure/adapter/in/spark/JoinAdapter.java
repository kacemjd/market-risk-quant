package com.kacemrisk.market.infrastructure.adapter.in.spark;

import com.kacemrisk.market.application.port.in.FetchHistoricalPricesCommand;
import com.kacemrisk.market.application.port.in.FetchHistoricalPricesUseCase;
import com.kacemrisk.market.domain.model.AssetClass;
import com.kacemrisk.market.domain.model.HistoricalPrice;
import com.kacemrisk.market.infrastructure.loader.PortfolioLoader;
import com.kacemrisk.market.infrastructure.model.RiskPositionSlim;
import com.kacemrisk.market.workflow.RunContext;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.api.java.JavaSparkContext;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.broadcast.Broadcast;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Encoders;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.stereotype.Component;

/**
 * Spark enrichment adapter — joins portfolio positions with historical market prices and produces a
 * {@link Dataset} of {@link RiskPositionSlim}s ready for distributed VaR.
 *
 * <h3>Phase execution</h3>
 *
 * <ol>
 *   <li><b>Phase A</b> — collect unique {@code ticker → assetClass} from the cached portfolio
 *       dataset
 *   <li><b>Phase B</b> — bulk-fetch prices via {@link FetchHistoricalPricesUseCase#fetch},
 *       serialise to {@code Map<ticker, double[]>} (sorted oldest→newest), broadcast to all
 *       executors
 *   <li><b>Phase C</b> — {@code flatMap} each portfolio row to a {@link RiskPositionSlim} using the
 *       broadcast. No shuffle required — the broadcast is already available on every executor. Rows
 *       with unresolvable asset classes are dropped silently.
 * </ol>
 *
 * <h3>Lifecycle</h3>
 *
 * The returned {@link EnrichedDataset} owns the price broadcast and the cached portfolio dataset.
 * The caller must invoke {@link EnrichedDataset#release()} after calibration is complete and before
 * the executor-side VaR computation begins — see {@link ComposeAdapter}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JoinAdapter {

    private final SparkSession spark;
    private final JavaSparkContext jsc;
    private final PortfolioLoader portfolioLoader;
    private final FetchHistoricalPricesUseCase fetchHistoricalPricesUseCase;

    /**
     * Enriches the portfolio with historical prices and returns a lazy {@link EnrichedDataset}.
     *
     * <p>The portfolio dataset is cached for Phase A re-use by Phase C. Both the cache and the
     * price broadcast are owned by the returned {@link EnrichedDataset} and released by the caller
     * via {@link EnrichedDataset#release()}.
     */
    public EnrichedDataset enrich(RunContext ctx) {

        Dataset<Row> portfolioDs = portfolioLoader.load().cache();

        if (portfolioDs.isEmpty()) {
            log.error("[JoinAdapter] Portfolio dataset is empty — aborting.");
            portfolioDs.unpersist();
            return new EnrichedDataset(
                    spark.emptyDataset(Encoders.kryo(RiskPositionSlim.class)), null, null);
        }

        // Phase A — collect unique ticker→assetClass (cache hit avoids second CSV read)
        Map<String, AssetClass> tickerAssetClass =
                portfolioDs.select("ticker", "assetClass").distinct().collectAsList().stream()
                        .flatMap(
                                r -> {
                                    AssetClass ac = AssetClass.of(r.getAs("assetClass"));
                                    if (ac == null) {
                                        log.warn(
                                                "[JoinAdapter] Unknown asset class '{}' for ticker"
                                                        + " '{}' — skipping",
                                                r.getAs("assetClass"),
                                                r.getAs("ticker"));
                                        return Stream.empty();
                                    }
                                    return Stream.of(Map.entry((String) r.getAs("ticker"), ac));
                                })
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey, Map.Entry::getValue, (a, b) -> a));
        log.info("[JoinAdapter] Phase A — {} unique ticker(s)", tickerAssetClass.size());

        // Phase B — bulk fetch + serialise to Map<ticker, double[]>
        var from = ctx.asOfDate().minusDays(ctx.historicalWindow());

        List<HistoricalPrice> rawPrices =
                fetchHistoricalPricesUseCase.fetch(
                        FetchHistoricalPricesCommand.builder()
                                .tickers(tickerAssetClass)
                                .from(from)
                                .to(ctx.asOfDate())
                                .build());

        if (rawPrices.isEmpty()) {
            log.error(
                    "[JoinAdapter] No prices loaded for window [{} → {}] — aborting.",
                    from,
                    ctx.asOfDate());
            portfolioDs.unpersist();
            return new EnrichedDataset(
                    spark.emptyDataset(Encoders.kryo(RiskPositionSlim.class)), null, null);
        }

        // Serialise List<HistoricalPrice> → Map<ticker, double[]> sorted oldest→newest
        Map<String, double[]> pricesByTicker =
                rawPrices.stream()
                        .collect(
                                Collectors.groupingBy(
                                        HistoricalPrice::getTicker,
                                        Collectors.collectingAndThen(
                                                Collectors.toList(),
                                                list ->
                                                        list.stream()
                                                                .sorted(
                                                                        Comparator.comparing(
                                                                                HistoricalPrice
                                                                                        ::getDate))
                                                                .mapToDouble(
                                                                        HistoricalPrice
                                                                                ::getClosePrice)
                                                                .toArray())));

        log.info(
                "[JoinAdapter] Phase B — {} ticker(s) serialised, {} total price records",
                pricesByTicker.size(),
                rawPrices.size());

        Broadcast<Map<String, double[]>> priceBroadcast = jsc.broadcast(pricesByTicker);

        Dataset<RiskPositionSlim> positions =
                portfolioDs.flatMap(
                        (FlatMapFunction<Row, RiskPositionSlim>)
                                row -> {
                                    String ticker = row.getAs("ticker");
                                    AssetClass ac = AssetClass.of(row.getAs("assetClass"));
                                    if (ac == null) {
                                        // unknown asset class — skip silently (already warned in
                                        // Phase A)
                                        return Collections.emptyIterator();
                                    }
                                    double[] history =
                                            priceBroadcast
                                                    .value()
                                                    .getOrDefault(ticker, new double[0]);
                                    double spot =
                                            history.length > 0 ? history[history.length - 1] : 0.0;
                                    return Collections.singletonList(
                                                    new RiskPositionSlim(
                                                            row.getAs("portfolioId"),
                                                            ticker,
                                                            row.getAs("quantity"),
                                                            ac,
                                                            spot))
                                            .iterator();
                                },
                        Encoders.kryo(RiskPositionSlim.class));

        return new EnrichedDataset(positions, priceBroadcast, portfolioDs);
    }
}
