# Screen 1 — Variant: LIMIT-FIRST

**Artifact:** [`assets/risk-desk-variant-limit-first.svg`](assets/risk-desk-variant-limit-first.svg) (1280 px static desktop mockup)

> **All values in the mockup are synthetic.** The mockup carries an on-canvas banner saying so. No figure, limit, exception or run id corresponds to a real portfolio or a real approved mandate.

---

## 1. Concept and rationale

This variant answers one question before any other: **is the desk inside its approved limits right now, and is the model that produced those numbers still trustworthy?**

Every other framing (risk decomposition, trend, attribution) is subordinated to that. The design follows three rules:

1. **Limit status is the headline, the measured value is the supporting detail.** Each metric card leads with a status chip (`WITHIN LIMIT`, `BREACH`, `NO LIMIT CONFIGURED`, `APPROXIMATION`) and a utilisation track, not with a number floating in space. A number without a limit is an unanswered question, and the layout says so instead of hiding it.

2. **A limit is never invented.** Where the mandate defines no limit, the utilisation track is replaced by an explicit hatched "not configured" state and the caption reads *"No utilisation can be computed"*. Where a limit is conceptually inapplicable (the 10-day approximation is not limit-monitored), a distinct dotted "not applicable" state is used. These two states are visually different from each other and from a zero-utilisation bar, because they mean different things:
   - **not configured** — the metric is measurable, the governance input is missing;
   - **not applicable** — governance deliberately does not monitor this figure.

3. **Unavailable is a first-class value.** The *Limit coverage on this screen* panel counts unconfigured and inapplicable limits alongside breaches, so the gap in limit coverage is itself a reported KPI (`5 not configured / not applicable`). A reviewer can see at a glance that the green picture at desk level is only partially governed.

### Method, once, page-wide

Method and distribution are stated once in the header band and explicitly scoped: *"METHOD (applies to every figure on this page) — Historical simulation · 501 obs (2y) · full revaluation · equal weights"*, and *"Distribution: empirical, unsmoothed · no parametric fit · no scaling except where stated."* Repeating methodology per tile invites drift between tiles; stating it once and scoping it with "applies to every figure on this page" makes a per-tile deviation impossible to introduce silently. The only deviation — square-root-of-time scaling for the 10-day figure — is called out on the card itself (`4.82 × √10`) and in the "no scaling except where stated" clause.

### Preserved measures

| Measure | Treatment in this variant |
|---|---|
| VaR 99% · 1d | Primary limit-monitored metric: 4.82 USD m vs 6.50 limit, 74% utilised, green |
| ES 97.5% · 1d | 5.41 USD m, measured and shown, but **no limit configured** — explicit state, no utilisation |
| VaR 99% · 10d (approx.) | 15.24 USD m, flagged `APPROXIMATION`, formula shown, **not applicable** for limits |
| Exceptions · 250d | 4 of 250, Basel traffic-light strip (green 0–4 / amber 5–9 / red ≥10), plus the forward-looking note "one more exception moves the desk to amber" |
| Method | Page-wide header band, scoped statement |
| Distribution | Page-wide header band, empirical/unsmoothed |
| Component VaR | Reconciled additive Euler allocation summing **exactly** to 4.82, with the arithmetic printed under the table |
| Diversification | **Separate** panel, explicitly "not a limit and not allocated", never netted into a component |
| Backtest | 60-day daily clean P&L against VaR and ES thresholds, 2 exceptions marked |

### Additivity and diversification

The component table shows *standalone* and *component (contribution)* VaR side by side. Contributions sum to the desk total with no residual, and the reconciliation line (`2.31 + 1.42 + 0.51 + 0.34 + 0.18 + 0.06 = 4.82 — reconciled, no residual`) is printed so the property is auditable from the screenshot alone. The diversification benefit (2.27 USD m, 32.0% of the 7.09 standalone sum) lives in its own card. It is deliberately **not** a table row and **not** a negative component: folding it into the decomposition would break additivity and imply it can be managed or limited, which it cannot.

### Palette use

| Colour | Role |
|---|---|
| `#1D9E75` green | Within limit; Basel green zone |
| `#EF9F27` amber | Approximation / scaled figure; near-limit band; synthetic-data banner |
| `#E24B4A` red | Breach; backtest exception; Basel red zone |
| `#7F77DD` purple | Neutral measured data — P&L bars, diversification, ES threshold |
| `#B4B2A9` grey | Not configured, not applicable, unavailable, and all non-semantic metadata |

Grey is reserved for absence, so "we don't know" can never be mistaken for "we're fine". Purple carries data that has no status attached to it, which keeps green/amber/red purely semantic.

---

## 2. Visual hierarchy

```
1. Header band (dark)          identity + page-wide method/distribution
2. Synthetic-data banner       amber, full width, unmissable
3. RISK VERSUS APPROVED LIMITS four cards, each: title -> status chip -> value -> limit -> utilisation
4. BACKTESTING 60 DAYS         wide chart, model-validity evidence directly under the limit row
5. WHERE THE RISK SITS         component table (2/3) + diversification and limit-coverage (1/3)
6. Footer                      provenance: run id, engine, timestamp, synthetic warning
```

Reading order is top-down and answers, in sequence: *are we within limits → can we trust the model that said so → what is driving the number → how much of this is actually governed*.

Within a card the order is deliberately **status before magnitude**: a trader or a risk manager scanning the row reads four chips in under a second and only then descends to figures. Type scale is compressed (38 px value, 13.5 px title, 10.5–12 px supporting) so that colour and chip position — not font size — carry the hierarchy.

---

## 3. Interaction notes (for the live implementation)

The SVG is static; these are the behaviours the layout is designed to accept without re-flow.

- **Limit cards** — hover reveals a provenance popover: mandate id/version, limit effective date, approver, last change. Click opens the limit history drawer. Cards in a `NOT CONFIGURED` state offer *"Request limit"* rather than a drill-down, and must never fabricate a provisional limit client-side.
- **Utilisation track** — the `limit` cap marker stays at a fixed x-position so 100% is at the same pixel on every card; a breach renders as overflow past the marker (the track is scaled to 125%), so breaches are detectable peripherally without reading the percentage.
- **Basel zone strip** — hover on a zone shows the exception count boundaries and the resulting multiplier add-on.
- **Backtest chart** — hover a bar for date, clean P&L, that day's VaR/ES, and exception flag. Click an exception bar to open the exception record (cause, sign-off state). Window selector (60d / 125d / 250d) replaces the chart body only. The VaR and ES threshold lines are step lines in the live version (VaR changes daily); the mockup flattens them because a single COB snapshot is shown.
- **Component table** — sortable by component VaR or utilisation; clicking a row drills into sub-factor decomposition. Sorting must never change the total row or break the printed reconciliation. Rows without a sub-limit sort last under "utilisation" sort, never as 0%.
- **Refresh / staleness** — the footer run id is the anchor. If a feed is stale or an engine run partially failed, the affected card degrades to a grey "unavailable" state with the reason, rather than showing the previous run's number.
- **Accessibility** — `role="img"` plus `aria-labelledby` pointing at `<title>`/`<desc>` on the static asset. In the live version each card is a labelled region; status is conveyed by chip text as well as colour, exception bars carry a marker dot and a text label, and the zone strip is annotated numerically. Keyboard order follows the reading order above.

---

## 4. Strengths and tradeoffs

**Strengths**

- The governance question is answered in the first screenful, with no drill-down.
- Missing limits are impossible to overlook and impossible to confuse with compliance; the coverage panel quantifies the gap.
- Method and distribution are stated exactly once, page-scoped, which removes per-tile ambiguity.
- Additivity is printed and auditable; diversification cannot be mistaken for an allocable or limitable quantity.
- Model validity (backtest) sits immediately under the limit row, so a green limit picture is always read next to the evidence that the model behind it is behaving.
- Colour semantics are strict: grey never means "good".

**Tradeoffs**

- **Status-first compresses magnitude.** Two desks both at "within limit" look alike even if one is at 20% and the other at 74%; utilisation bars mitigate this but the chip dominates the glance.
- **No trend.** There is no VaR time series — only 60 days of P&L. A steadily rising utilisation inside the limit reads the same as a flat one. A sparkline in each card is the obvious follow-up, at the cost of card density.
- **Single desk, single currency, single COB.** The layout does not address desk roll-up or multi-entity aggregation; a hierarchy variant is needed for that.
- **Screen real estate spent on absence.** Roughly a fifth of the layout is devoted to "not configured / not applicable" states. In a fully governed book that space is under-used — deliberate, but a real cost.
- **The 10-day approximation is prominent for a figure nobody manages to.** It earns its place only because an unflagged √10 number elsewhere in the org is a known failure mode; a reader could reasonably argue it belongs in a secondary panel.
- **Traffic-light framing can breed complacency.** "Green" is a zone boundary, not a safety statement; the "one more exception moves the desk to amber" caption is a partial antidote.

---

## 5. React + TypeScript + Vite implications

### Domain model — absence is modelled, not defaulted

The single most important implication is that the limit is an optional, discriminated value and the UI must be forced by the type system to handle each case.

```ts
export type Money = { amount: number; ccy: 'USD' };

export type LimitState =
  | { kind: 'configured'; limit: Money; mandateId: string; effectiveFrom: string }
  | { kind: 'not-configured'; reason: string }
  | { kind: 'not-applicable'; reason: string }
  | { kind: 'unavailable'; reason: string };   // limit service down / stale

export type MeasureState =
  | { kind: 'measured'; value: Money }
  | { kind: 'unavailable'; reason: string };

export interface RiskMeasure {
  id: 'var99_1d' | 'es975_1d' | 'var99_10d_approx' | 'exceptions_250d';
  label: string;
  measure: MeasureState;
  limit: LimitState;
  approximation?: { basis: string; formula: string }; // e.g. √10 scaling
}
```

- `limit?: number` is banned. An optional number invites `limit ?? 0`, which silently manufactures a 100%-utilised or 0%-utilised state. The discriminated union makes the "no limit" branch unrepresentable-as-a-number.
- Utilisation is a function that **cannot** return a number when the limit is absent:

```ts
export type Utilisation =
  | { kind: 'computed'; ratio: number; status: 'within' | 'near' | 'breach' }
  | { kind: 'not-computable'; because: LimitState['kind'] };
```

- Enable `strict`, `noUncheckedIndexedAccess`, and `exactOptionalPropertyTypes` in `tsconfig`. Add an ESLint rule banning `??` / `||` fallbacks on limit and measure fields.

### Components

- `<LimitCard>` renders a discriminated switch with an exhaustiveness guard (`assertNever`), so adding a fifth limit state is a compile error in every card, not a silent grey box.
- `<UtilisationTrack>` takes `Utilisation`, not `number | undefined`; the `not-computable` branch renders the hatched/dotted patterns.
- `<StatusChip>` derives label and colour from one `status` union — colour is never passed in by the caller, which prevents a red chip reading "WITHIN".
- `<ComponentVaRTable>` asserts reconciliation at render time in dev: `expect(sum(components) ≈ total, tol 0.005)`, surfacing a visible reconciliation-failed banner in prod rather than a silently wrong table.
- `<DiversificationPanel>` receives `{ standaloneSum, diversifiedTotal }` and derives the benefit; it is structurally incapable of being rendered as a table row.

### Charting

- The backtest chart is ~60 bars plus two threshold lines — hand-rolled SVG with `d3-scale` only (no `d3-selection`, no chart library). This keeps the DOM identical to the mockup, keeps bundle size low, and keeps `<title>`/`aria` control in React's hands.
- Scales as pure functions in a `useMemo`; bars as a mapped array of `<rect>`. Tooltips as a React-positioned overlay, not SVG `<title>` elements, so they are keyboard-reachable.

### Theming and tokens

- Palette as CSS custom properties (`--risk-green: #1D9E75`, …) in a single `tokens.css`, consumed by both CSS and the SVG chart via `fill="var(--risk-green)"`. The static asset intentionally uses literal hex so it stays portable outside the app; the app must not re-use the hex values inline.
- Semantic aliases (`--status-within`, `--status-breach`, `--status-unknown`) sit on top of the raw palette, so a palette change never requires touching component code.

### Data, state, and Vite specifics

- Server state via TanStack Query keyed by `[deskId, cobDate, runId]`; the run id in the footer comes from the same payload, so the screen can never mix two engine runs.
- A partial-failure-tolerant DTO: the Java service should return per-measure status rather than failing the whole payload, mapping cleanly onto `MeasureState`/`LimitState`. Decode with `zod` at the boundary and fail loudly on unknown `kind` values — a new backend state must not fall through to "looks fine".
- Vite: `@vitejs/plugin-react`, path aliases for `@/domain`, `vite-plugin-svgr` if the mockup is imported as a component for visual-regression baselines. Manual chunking to split `d3-scale` and the decimal library out of the initial bundle.
- Use a decimal type (or integer minor units) for money and limits; `0.1 + 0.2` style drift will break the printed additivity reconciliation.
- Tests: Vitest + Testing Library snapshot per `LimitState` variant; a property test asserting `sum(componentVaR) === totalVaR` for generated inputs; Playwright visual regression against this SVG's layout regions; an axe check on each card state.
