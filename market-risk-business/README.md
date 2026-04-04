# market-risk-business

Pure domain module — no framework dependencies.  
Contains the core risk models and all BDD specifications.

---

## Parametric Value-at-Risk

### 1. Covariance matrix

Given a vector of per-asset annualised volatilities **σ** and a correlation matrix **ρ**, the covariance matrix is:

$$
\Sigma = \operatorname{diag}(\sigma) \cdot \rho \cdot \operatorname{diag}(\sigma)
$$

$$
\Sigma_{ij} = \sigma_i \cdot \rho_{ij} \cdot \sigma_j
$$

---

### 2. Portfolio variance (quadratic form)

The P&L variance of a linear portfolio is the quadratic form:

$$
\sigma_P^2 = \delta^\top \cdot \Sigma \cdot \delta
= \sum_{i=1}^{n} \sum_{j=1}^{n} \delta_i \, \Sigma_{ij} \, \delta_j
$$

where **δ** is the vector of position deltas (sensitivity of P&L to a unit move in each risk factor).

**Mono-asset simplification** — when *n = 1*, Σ collapses to the scalar σ²:

$$
\sigma_P^2 = \delta^2 \cdot \sigma^2
\implies \sigma_P = |\delta| \cdot \sigma
$$

---

### 3. Parametric VaR

Assuming normally distributed P&L, the one-sided VaR at confidence level α is:

$$
\text{VaR}_\alpha = \mu_P + \Phi^{-1}(\alpha) \cdot \sigma_P
$$

Expanding with the quadratic form for σ_P:

$$
\text{VaR}_\alpha = \mu_P + \Phi^{-1}(\alpha) \cdot \sqrt{\delta^\top \Sigma \, \delta}
$$

For short holding periods, the expected return μ_P is typically assumed to be zero:

$$
\text{VaR}_\alpha \approx \Phi^{-1}(\alpha) \cdot \sqrt{\delta^\top \Sigma \, \delta}
$$

| Symbol | Meaning |
|--------|---------|
| α | Confidence level (e.g. 0.95, 0.99) |
| Φ⁻¹(α) | Inverse standard-normal CDF (e.g. ≈ 1.645 at 95 %, ≈ 2.326 at 99 %) |
| μ_P | Expected portfolio P&L (mean return) |
| δ | Delta vector — P&L sensitivity per risk factor |
| Σ | Covariance matrix of risk-factor returns |
| σ_P | Portfolio P&L standard deviation |

---

### 4. Quadratic form — implementation comparison

Both implementations compute **δᵀ · Σ · δ** identically; they differ only in allocation strategy.

#### Loop (preferred)

```
variance = 0
for i in 0..n:
    for j in 0..n:
        variance += δ[i] * Σ[i][j] * δ[j]
```

Zero heap allocation — operates directly on primitive arrays.

#### Commons Math (reference)

```
Σδ    = Σ · δ          # matrix-vector product  → new double[]
δᵀΣδ  = δ · (Σδ)       # dot product
```

Allocates `ArrayRealVector` and `Array2DRowRealMatrix` on every call.

#### Benchmark results (JMH, AverageTime, µs/op)

| n | loop | commonsMatrix | speedup |
|-----|-------|---------------|---------|
| 5 | 0.073 | 0.712 | ~10× |
| 50 | 5.420 | 9.989 | ~2× |
| 500 | 551.8 | 1359.1 | ~2.5× |

Run benchmarks:

```bash
mvn test -pl market-risk-business \
  -Dtest=VarianceComputationBenchmark#main \
  -DfailIfNoTests=false
```

---

## Market Data Calibration

`MarketDataCalibrationService.calibrateFromPrices` runs the following pipeline on a map of historical closing prices.

### 1. Log-returns

$$
r_t = \ln\!\left(\frac{S_t}{S_{t-1}}\right)
$$

### 2. Annualised volatility

$$
\sigma_{\text{ann}} = \sqrt{\frac{\sum_{i=1}^{n}(r_i - \bar{r})^2}{n - 1}} \times \sqrt{252}
$$

### 3. Pearson correlation matrix

$$
\rho_{xy} = \frac{\displaystyle\sum_{i=1}^{n}(x_i - \bar{x})(y_i - \bar{y})}
                 {\sqrt{\displaystyle\sum_{i=1}^{n}(x_i - \bar{x})^2 \;\times\; \displaystyle\sum_{i=1}^{n}(y_i - \bar{y})^2}}
$$

### 4. Covariance matrix

$$
\Sigma_{ij} = \rho_{ij} \cdot \sigma_i \cdot \sigma_j
$$

| Symbol | Meaning |
|--------|---------|
| $S_t$ | Closing price at time *t* |
| $r_t$ | Log-return at time *t* |
| $\bar{r}$ | Mean log-return over the series |
| $\sigma_{\text{ann}}$ | Annualised volatility (252 trading days) |
| $\rho_{ij}$ | Pearson correlation between risk factors *i* and *j* |
| $\Sigma_{ij}$ | Covariance between risk factors *i* and *j* |

---

## Module structure

```
domain/
  model/          # Portfolio, Position, MarketData, AssetClass
  service/
    simulation/   # VaRCalculator, ParametricVaRCalculator
    calibration/  # MarketDataCalibrator
    pricing/
  exception/

application/
  port/in/        # Use-case interfaces (CalculateVaRUseCase, CalibrateMarketDataUseCase)
```

BDD feature files live under `src/test/resources/features/` and are driven by Cucumber via `CucumberRunner`.

