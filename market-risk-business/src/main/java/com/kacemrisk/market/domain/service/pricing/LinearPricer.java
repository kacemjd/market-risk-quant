package com.kacemrisk.market.domain.service.pricing;

import com.kacemrisk.market.domain.model.Position;

/**
 * Basic linear pricer for equities and linear derivatives (futures).
 * <p>
 * P&L = quantity * (Delta * delta S)
 */
public class LinearPricer implements Pricer {

    @Override
    public double calculatePnl(Position position, double returnShock) {
        double deltaS = PricingUtils.logReturnToAbsoluteShock(position.getSpotPrice(), returnShock);
        return position.getQuantity() * position.getDelta() * deltaS;
    }
}

