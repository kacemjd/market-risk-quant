# Production Roadmap

> Last updated: April 2026 — Post Phase 1 TRIM

---

## Current State

Phase 1 TRIM delivered three VaR methodologies (Parametric, Monte Carlo, Historical Simulation), Expected Shortfall (CVaR), Delta-Gamma pricing, and a Spark-based market data calibration pipeline. The hexagonal architecture is clean and enforced. Five critical/high bugs have been fixed.

**What's done:**

| Feature | Status |
|---|---|
| Parametric VaR (closed-form Gaussian) | ✅ |
| Monte Carlo VaR (Cholesky-based GBM) | ✅ |
| Historical Simulation VaR (full-revaluation) | ✅ |
| Expected Shortfall / CVaR (all 3 methods) | ✅ |
| Delta-Gamma pricing layer | ✅ |
| Spark ingestion pipeline (CSV → log returns → vol → ρ → Σ) | ✅ |
| 3 trigger modes (REST, Kafka, Cron) | ✅ |
| BDD tests (Cucumber) + JMH benchmarks | ✅ |
| Hexagonal architecture (framework-free domain) | ✅ |

**What's open:** 9 tech-debt issues (see [Architecture Review](architecture-review.md))

---

## Sprint 1 — Tech Debt & Hygiene

**Goal:** Clean codebase, zero open issues, CI-ready.

| # | Task | Effort | Issue |
|---|---|---|---|
| 1.1 | **Rename `Portoflio` → `Portfolio`** across all modules | 30 min | #6 |
| 1.2 | **Make `VaRAggregator` immutable** — constructor-inject `alpha`, remove `atConfidence()` mutation | 1h | #10 |
| 1.3 | **Clean up orphaned ports** — delete `RunMonteCarloVaRUseCase`, wire `CalibrateMarketDataUseCase` to its service | 1h | #8, #9 |
| 1.4 | **Add root Java package** — `com.kacemrisk.market.{domain,application,workflow,infrastructure}`, update `groupId` | 2h | #11 |
| 1.5 | **REST validation & error handling** — `@Valid` on controller, `@RestControllerAdvice`, proper HTTP status mapping | 1h | #12, #13 |
| 1.6 | **Add `local` Maven profile** — override Spark `provided` → `compile` for dev mode | 30 min | #14 |
| 1.7 | **Delete dead `MonteCarloVaRPipeline`** — superseded by `VaRCalculationPipeline` | 10 min | — |
| 1.8 | **Add GitHub Actions CI** — `mvn clean verify` on push, coverage badge | 1h | — |

**Estimated total:** 1 week

---  

## Sprint 2 — Persistence, API & Docker

**Goal:** Replace in-memory stubs with real storage, add result query API, dockerize the full stack.

| # | Task | Effort |
|---|---|---|
| 2.1 | **PostgreSQL/TimescaleDB persistence** — Spring Data JPA entities for `MarketDataEntity` and `VaRResultEntity`, replace `InMemoryMarketDataRepository` and `InMemoryPortfolioRepository` | 3–4h |
| 2.2 | **VaR Result Query API** — `GET /results?portfolioId=&from=&to=&method=` behind `rest` profile, backed by JPA | 2h |
| 2.3 | **OpenAPI spec** — add `springdoc-openapi` for auto-generated Swagger docs | 1h |
| 2.4 | **Kafka VaR result publisher** — `KafkaTemplate<String, VaRResult>` adapter on `var-results` topic, conditional on `kafka` profile | 2h |
| 2.5 | **Idempotent scenario runs** — `correlationId` uniqueness check, run status (`PENDING`, `COMPLETED`, `FAILED`) | 1h |
| 2.6 | **`docker-compose.yml`** — app + TimescaleDB + Kafka (Redpanda) + init scripts | 2h |
| 2.7 | **Flyway migrations** — versioned schema for `market_data`, `var_results`, `scenario_runs` | 1h |

**Estimated total:** 1.5 weeks

---

## Sprint 3 — Quant Depth & Scalability

**Goal:** Add features that signal quant maturity to a hiring manager reviewing VaR models.

| # | Task | Effort | Impact |
|---|---|---|---|
| 3.1 | **Fix `ComposeAdapter.collectAsList()`** — replace with `groupByKey` + `mapPartitions` for distributed VaR | 3h | #7 |
| 3.2 | **Component & Marginal VaR** — Euler allocation `∂VaR/∂w_i`, add `componentVaR` map to `VaRResult` | 4h | High — Natixis econometrics validates this |
| 3.3 | **Filtered Historical Simulation (FHS)** — add `FILTERED_HISTORICAL` to `VaRMethod`, EWMA vol-scaling of historical returns | 4h | High — FRTB IMA hybrid approach |
| 3.4 | **Stress Testing framework** — `StressScenario` model, `StressTestService`, regulatory shocks (equity crash, rate shift, FX dislocation) | 4h | Medium |
| 3.5 | **Multi-currency support** — FX risk factor, cross-currency P&L conversion | 3h | Medium — shows multi-asset capability |

**Estimated total:** 2 weeks

---

## Sprint 4 — Observability & Documentation

**Goal:** Make the platform operationally visible and the codebase presentation-ready.

| # | Task | Effort |
|---|---|---|
| 4.1 | **Micrometer metrics** — `@Timed` on `VaRService`, custom gauges for scenario latency, VaR by portfolio, calibration time | 2h |
| 4.2 | **Prometheus + Grafana** — actuator endpoint, sample dashboard JSON in `docs/grafana/` | 2h |
| 4.3 | **OpenTelemetry tracing** — `correlationId` as trace baggage, Jaeger container in docker-compose | 2h |
| 4.4 | **Health check** — `/actuator/health` with Spark session, DB connectivity, Kafka broker indicators | 1h |
| 4.5 | **Documentation overhaul** — see structure below | 3h |

**Estimated total:** 1.5 weeks

---

## Target Documentation Structure

```
docs/
├── architecture-review.md          ← Updated scorecard & issue register
├── roadmap.md                      ← This file
├── quant-methodology.md            ← NEW: Math for all VaR methods, ES, Component VaR, FHS
├── api-reference.md                ← NEW: Link to OpenAPI spec, example requests/responses
├── deployment.md                   ← NEW: docker-compose, Spark cluster modes, env vars
└── adr/                            ← NEW: Architecture Decision Records
    ├── 001-hexagonal-architecture.md
    ├── 002-spark-over-flink.md
    ├── 003-timescaledb-for-timeseries.md
    └── 004-var-method-selection-strategy.md
```

---

## Priority Matrix

```
                    HIGH IMPACT
                        │
    ┌───────────────────┼───────────────────┐
    │                   │                   │
    │  Sprint 1         │  Sprint 3         │
    │  Tech debt        │  Component VaR    │
    │  CI pipeline      │  FHS              │
    │  Portfolio typo   │  Stress Testing   │
    │                   │                   │
LOW ├───────────────────┼───────────────────┤ HIGH
EFF │                   │                   │ EFFORT
    │  Sprint 4         │  Sprint 2         │
    │  Metrics          │  Persistence      │
    │  Health checks    │  Docker stack     │
    │  Docs             │  Kafka publisher  │
    │                   │                   │
    └───────────────────┼───────────────────┘
                        │
                    LOW IMPACT
```

**Order of execution:** Sprint 1 → Sprint 2 → Sprint 3 → Sprint 4

Sprint 1 is non-negotiable — the typo and dead code are visible in 10 seconds on GitHub.  
Sprint 3 is the Natixis differentiator — Component VaR and FHS are exactly what their econometrics team reviews.  
Sprints 2 and 4 complete the platform story (persistence, observability, Docker).

---

## Definition of "Production-Ready"

The platform will be considered production-ready when:

- [ ] Zero open tech-debt issues
- [ ] All VaR results persisted (not in-memory)
- [ ] Result query API with OpenAPI spec
- [ ] Full docker-compose stack (app + DB + Kafka + monitoring)
- [ ] Component VaR decomposition
- [ ] Stress testing framework
- [ ] Metrics + tracing + health checks
- [ ] CI pipeline green with coverage badge
- [ ] Documentation complete (quant methodology, API reference, deployment guide, ADRs)

