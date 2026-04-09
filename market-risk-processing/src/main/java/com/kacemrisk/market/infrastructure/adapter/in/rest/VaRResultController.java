package com.kacemrisk.market.infrastructure.adapter.in.rest;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Profile("rest")
@RestController
@RequestMapping("/results")
public class VaRResultController {

    @Autowired(required = false)
    private JdbcTemplate jdbc;

    @GetMapping
    public List<VaRResultResponse> query(
            @RequestParam(required = false) String portfolioId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String method) {

        if (jdbc == null) return List.of();

        StringBuilder sql = new StringBuilder("""
                SELECT correlation_id, portfolio_id, as_of_date, method,
                       var_amount, expected_shortfall, alpha, mean_pnl, std_dev_pnl, num_scenarios
                FROM var_results
                WHERE 1=1
                """);
        List<Object> params = new ArrayList<>();

        if (portfolioId != null) { sql.append(" AND portfolio_id = ?"); params.add(portfolioId); }
        if (from != null)        { sql.append(" AND as_of_date >= ?"); params.add(from.toString()); }
        if (to != null)          { sql.append(" AND as_of_date <= ?"); params.add(to.toString()); }
        if (method != null)      { sql.append(" AND method = ?");      params.add(method.toUpperCase()); }

        sql.append(" ORDER BY ts DESC");

        List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
        log.debug("VaR result query returned {} rows", rows.size());

        return rows.stream().map(r -> VaRResultResponse.builder()
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
                .build()).toList();
    }

    private double toDouble(Object v) {
        return v instanceof Number n ? n.doubleValue() : 0.0;
    }
}

