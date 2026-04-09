package com.kacemrisk.market.domain.service.pricing;

import com.kacemrisk.market.domain.model.Position;

/**
 * Strategy interface for Instrument Valuation.
 * Decouples P&L calculation from the scenario generation engine.
 */
public interface Pricer {

    /**
     * Calculates the P&L of a position given a raw market shock.
     *
     * @param position    the instrument to price
     * @param returnShock the simulated log-return shock for this instrument's underlying risk factor
     * @return the computed profit or loss
     */
    double calculatePnl(Position position, double returnShock);
}

