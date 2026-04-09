package com.kacemrisk.market.domain.model;

/**
 * Enumeration of supported VaR simulation methodologies.
 *
 * <ul>
 *   <li>{@link #PARAMETRIC}  — closed-form, assumes Gaussian P&L (fast, linear portfolios)</li>
 *   <li>{@link #MONTE_CARLO} — N correlated GBM paths via Cholesky (configurable paths/horizon)</li>
 *   <li>{@link #HISTORICAL}  — full-revaluation replay of T historical days (model-free, captures fat tails)</li>
 * </ul>
 */
public enum VaRMethod {
    PARAMETRIC,
    MONTE_CARLO,
    HISTORICAL
}

