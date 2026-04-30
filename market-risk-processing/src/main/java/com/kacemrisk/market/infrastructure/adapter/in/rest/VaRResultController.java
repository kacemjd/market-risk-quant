package com.kacemrisk.market.infrastructure.adapter.in.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/results")
@Tag(
        name = "VaR Results",
        description = "Query persisted VaR results from QuestDB (requires questdb profile)")
public class VaRResultController {

    @Autowired(required = false)
    private JdbcTemplate jdbc;

    @Operation(
            summary = "Query VaR results",
            description =
                    "Returns stored VaR results filtered by portfolio, date range and method."
                            + " Returns empty list if QuestDB profile is not active.")
    @GetMapping
    public List<VaRResultResponse> query(
            @Parameter(description = "Portfolio ID, e.g. PTFL-001") @RequestParam(required = false)
                    String portfolioId,
            @Parameter(description = "From date (inclusive), YYYY-MM-DD")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "To date (inclusive), YYYY-MM-DD")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(description = "VaR method: MONTE_CARLO or PARAMETRIC")
                    @RequestParam(required = false)
                    String method) {

        if (jdbc == null) return List.of();

        StringBuilder sql =
                new StringBuilder(
                        """
SELECT correlation_id, portfolio_id, as_of_date, method,
       var_amount, expected_shortfall, alpha, mean_pnl, std_dev_pnl, num_scenarios
FROM var_results
WHERE 1=1
""");
        List<Object> params = new ArrayList<>();

        if (portfolioId != null) {
            sql.append(" AND portfolio_id = ?");
            params.add(portfolioId);
        }
        if (from != null) {
            sql.append(" AND as_of_date >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND as_of_date <= ?");
            params.add(to.toString());
        }
        if (method != null) {
            sql.append(" AND method = ?");
            params.add(method.toUpperCase());
        }

        sql.append(" ORDER BY ts DESC");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        log.debug("VaR result query returned {} rows", rows.size());

        return rows.stream()
                .map(
                        r ->
                                VaRResultResponse.builder()
                                        .correlationId((String) r.get("correlation_id"))
                                        .portfolioId((String) r.get("portfolio_id"))
                                        .asOfDate((String) r.get("as_of_date"))
                                        .method((String) r.get("method"))
                                        .var(toDouble(r.get("var_amount")))
                                        .expectedShortfall(toDouble(r.get("expected_shortfall")))
                                        .alpha(toDouble(r.get("alpha")))
                                        .meanPnL(toDouble(r.get("mean_pnl")))
                                        .stdDevPnL(toDouble(r.get("std_dev_pnl")))
                                        .numScenarios(((Number) r.get("num_scenarios")).intValue())
                                        .build())
                .toList();
    }

    @Operation(
            summary = "Historical VaR time series",
            description =
                    """
Returns a chronological (oldest → newest) VaR time series for a given portfolio.
Includes summary statistics (min, max, avg, latest VaR) plus the full data series.
Requires the **questdb** profile — returns 503 otherwise.
""")
    @GetMapping("/history/{portfolioId}")
    public ResponseEntity<VaRHistoryResponse> history(
            @Parameter(description = "Portfolio ID, e.g. PTFL-001", required = true) @PathVariable
                    String portfolioId,
            @Parameter(description = "From date (inclusive), YYYY-MM-DD")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @Parameter(description = "To date (inclusive), YYYY-MM-DD")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate to,
            @Parameter(description = "VaR method: MONTE_CARLO or PARAMETRIC (default: MONTE_CARLO)")
                    @RequestParam(defaultValue = "MONTE_CARLO")
                    String method) {

        if (jdbc == null) return ResponseEntity.status(503).build(); // QuestDB not active

        StringBuilder sql =
                new StringBuilder(
                        """
                        SELECT as_of_date, var_amount, expected_shortfall, alpha
                        FROM var_results
                        WHERE portfolio_id = ? AND method = ?
                        """);
        List<Object> params = new ArrayList<>(List.of(portfolioId, method.toUpperCase()));

        if (from != null) {
            sql.append(" AND as_of_date >= ?");
            params.add(from.toString());
        }
        if (to != null) {
            sql.append(" AND as_of_date <= ?");
            params.add(to.toString());
        }
        sql.append(" ORDER BY as_of_date ASC");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        log.debug("Historical VaR query for {} returned {} rows", portfolioId, rows.size());

        List<VaRHistoryResponse.VaRDataPoint> series =
                rows.stream()
                        .map(
                                r ->
                                        VaRHistoryResponse.VaRDataPoint.builder()
                                                .asOfDate((String) r.get("as_of_date"))
                                                .var(toDouble(r.get("var_amount")))
                                                .expectedShortfall(
                                                        toDouble(r.get("expected_shortfall")))
                                                .alpha(toDouble(r.get("alpha")))
                                                .build())
                        .toList();

        double[] vars =
                series.stream().mapToDouble(VaRHistoryResponse.VaRDataPoint::getVar).toArray();
        OptionalDouble min =
                series.stream().mapToDouble(VaRHistoryResponse.VaRDataPoint::getVar).min();
        OptionalDouble max =
                series.stream().mapToDouble(VaRHistoryResponse.VaRDataPoint::getVar).max();
        OptionalDouble avg =
                series.stream().mapToDouble(VaRHistoryResponse.VaRDataPoint::getVar).average();

        VaRHistoryResponse response =
                VaRHistoryResponse.builder()
                        .portfolioId(portfolioId)
                        .method(method.toUpperCase())
                        .totalPoints(series.size())
                        .latestVar(vars.length > 0 ? vars[vars.length - 1] : 0.0)
                        .minVar(min.orElse(0.0))
                        .maxVar(max.orElse(0.0))
                        .avgVar(avg.orElse(0.0))
                        .series(series)
                        .build();

        return ResponseEntity.ok(response);
    }

    private double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }
}
