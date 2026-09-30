# Risk Desk Variant: Compact Terminal

## Rationale

This alternative treats Screen 1 as an expert operating surface rather than a collection of dashboard cards. A persistent graphite navigation rail, one global method row, a continuous metric band, and three ruled data regions create a compact browser UI with terminal-like scan speed without imitating a command line.

The design is optimized for a risk manager moving through the same sequence repeatedly: confirm method and date, scan headline limits, inspect the distribution tail, identify additive drivers, separate diversification from contribution, and verify recent model behavior. All displayed figures are explicitly illustrative synthetic data; the mockup makes no performance, compliance, or model-quality claim.

## Information hierarchy

1. **Global context:** portfolio, as-of date, base currency, observation window, and a single global method selector.
2. **Headline measures:** VaR 99% 1d, ES 97.5% 1d, approximate VaR 99% 10d, and 250-day exceptions share one uninterrupted comparison band.
3. **Distribution evidence:** the P&L histogram places VaR and ES directly on the loss tail.
4. **Risk attribution:** component VaR is a dense reconciled table. The five contributions add to `$2.84m`, matching portfolio VaR.
5. **Diversification:** standalone sum, portfolio VaR, and benefit appear in a separate ruled strip so diversification is not mistaken for another additive component.
6. **Model behavior:** the 60-day backtest remains visible in the right rail, with threshold breaches and traffic-light reference context.

## Interaction notes

- The method selector is global; changing it should refresh headline measures, distribution, allocation, diversification, and backtest together.
- Keyboard users can open method selection with `G M` and global search with `/`. Production controls should use real buttons/selects with visible focus states and `aria-keyshortcuts`.
- Table rows should support arrow-key movement and Enter to open factor detail without making the entire row a hidden click target.
- Hover or keyboard focus on histogram bins and backtest points should reveal exact date/value tooltips. VaR and ES markers remain visible without hover.
- The backtest should allow a compact 60/250-day toggle, but 60 days is the Screen 1 default.
- Loading should preserve the ruled layout with stable row heights. Errors should remain local to the affected region and name the failed dataset and retry action.

## Strengths and tradeoffs

**Strengths**

- High data density with a clear left-to-right, top-to-bottom operating path.
- Direct reconciliation between headline VaR and additive component VaR.
- Persistent backtest evidence without scrolling or opening another card.
- Color is semantic and restrained: `#7F77DD` primary data, `#E24B4A` breaches/tail limits, `#EF9F27` approximation/caution, `#1D9E75` reconciled/within-range states, and `#B4B2A9` structure.
- Square corners and continuous rules make the screen feel precise while retaining standard browser navigation and controls.

**Tradeoffs**

- The density is best suited to trained users and requires careful progressive disclosure for factor detail.
- At narrower widths, the persistent backtest rail cannot simply shrink; it should move below the distribution rather than compress labels.
- Monospace numerals improve alignment but should be limited to values and shortcuts, not used as a general aesthetic.
- The compact layout leaves less room for explanatory copy, so definitions and methodology belong in contextual help or a dedicated model panel.

## React + TypeScript + Vite implications

- Build the screen from semantic regions such as `RiskHeader`, `MethodBar`, `RiskMetricBand`, `PnlDistribution`, `ComponentVarTable`, `DiversificationStrip`, and `BacktestPanel`.
- Keep one typed `RiskViewState` at the screen boundary so method, portfolio, date, currency, and window changes invalidate every dependent query consistently.
- Model the allocation as `ComponentVarRow[]` and assert reconciliation within an explicit tolerance before rendering the green reconciled state. A failed check should display an error state, not silently round to the portfolio total.
- Keep diversification in its own type and component rather than appending a synthetic row to component VaR.
- Use SVG for charts with descriptive titles, axis labels, focusable data points, and a non-visual data-table alternative. The static asset is a composition reference, not reusable chart code.
- Use CSS Grid for the desktop shell (`minmax(0, 790fr) minmax(360px, 434fr)`) and switch to a single column at the point where either chart loses legibility. Preserve table columns with horizontal overflow before hiding data.
- Define the five supplied colors as role-based CSS custom properties rather than component-local literals. Add neutral surface, rule, and text tokens with contrast checked against each background.
- Use tabular numerals (`font-variant-numeric: tabular-nums`) for measures and table values. Keep the normal UI stack for labels and navigation.
- Drive tooltips, row selection, and keyboard commands from accessible DOM controls layered with or adjacent to chart SVG, rather than relying on pointer-only SVG events.
- Mark fixtures and Storybook/demo data as `synthetic: true`, and render the illustrative-data disclosure whenever that flag is present.
