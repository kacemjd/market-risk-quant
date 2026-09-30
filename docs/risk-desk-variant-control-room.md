# Risk desk variant: Control Room

This alternative treats Screen 1 as an evidence-led operating surface for a risk manager, not a collection of summary cards. A compact command bar establishes portfolio, date, and data state; a page-wide method rail makes the active model impossible to miss; and a ledger of four headline measures leads directly into the supporting distribution, allocation, diversification, and backtest evidence. Every displayed value is explicitly **illustrative / synthetic**.

## Hierarchy

1. **Context and control:** portfolio, as-of date, data completeness, and one method selection that applies across the page.
2. **Current risk:** VaR 99% 1d, ES 97.5% 1d, approximate VaR 99% 10d, and 250-day exceptions share one scan line.
3. **Evidence:** the P&L distribution and VaR/ES thresholds occupy the largest region.
4. **Reconciliation:** additive component VaR visibly sums to the €12.8m total; diversification is intentionally separated so it cannot be mistaken for a component.
5. **Monitoring:** the 60-day backtest closes the page as a continuous evidence strip with synthetic exceptions highlighted.

## Interaction notes

- Changing the method in the full-width selector should refresh all dependent metrics and evidence together, with a visible loading state and retained prior values until the new result is ready.
- Hover or keyboard focus on a distribution bin or backtest point should reveal date, P&L, threshold, method, and observation source. Exceptions should remain identifiable without color alone.
- Selecting a component row should cross-highlight its contribution and open the underlying positions or factor evidence in a detail drawer.
- The reconciliation indicator should expose the exact sum and rounding delta; a non-zero material delta should become an explicit error state rather than silently showing success.
- The synthetic-data disclosure should remain persistent in prototypes and fixtures, not disappear when the viewport scrolls.

## Strengths and tradeoffs

**Strengths:** high information density without visual noise; strong method provenance; immediate traceability from totals to evidence; additive component VaR and diversification are semantically unambiguous; restrained color reserves red, amber, and green for meaning.

**Tradeoffs:** the desktop composition assumes a wide operating viewport; detailed labels require careful responsive reflow; the evidence-first layout gives less space to narrative guidance; and synchronized method changes require robust request-state handling.

## Frontend implications

For React + TypeScript + Vite, model the method selection as page-level typed state and derive a single query key from portfolio, as-of date, method, window, and currency. Keep `RiskMetricLedger`, `PnlDistribution`, `ComponentVarReconciliation`, `DiversificationSummary`, and `BacktestChart` presentational, with one screen controller responsible for coordinated loading, error, stale-data, and refresh states. Use SVG charts with semantic labels and keyboard-addressable data points; share numeric formatting and synthetic-data disclosure components; and validate at runtime that component contributions sum to total VaR within a declared rounding tolerance. CSS Grid can preserve the 780/440 evidence split on desktop and collapse it into a single reading order at narrower widths without changing the information hierarchy.
