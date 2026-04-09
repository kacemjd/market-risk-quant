-- VaR results time-series — one row per portfolio per scenario run
CREATE TABLE IF NOT EXISTS var_results (
    correlation_id      SYMBOL,
    portfolio_id        SYMBOL,
    as_of_date          SYMBOL,
    method              SYMBOL,
    var_amount          DOUBLE,
    expected_shortfall  DOUBLE,
    alpha               DOUBLE,
    mean_pnl            DOUBLE,
    std_dev_pnl         DOUBLE,
    num_scenarios       INT,
    ts                  TIMESTAMP
) TIMESTAMP(ts) PARTITION BY DAY WAL;

-- Per-ticker calibrated volatilities — audit trail of market data calibration
CREATE TABLE IF NOT EXISTS market_calibration (
    as_of_date  SYMBOL,
    ticker      SYMBOL,
    volatility  DOUBLE,
    ts          TIMESTAMP
) TIMESTAMP(ts) PARTITION BY DAY WAL;

