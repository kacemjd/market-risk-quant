package domain.service.pricing;

import domain.model.Position;

/**
 * Non-linear Taylor expansion pricer for options.
 * <p>
 * P&L = quantity * (Delta * delta S + 0.5 * Gamma * (delta S)^2)
 */
public class DeltaGammaPricer implements Pricer {

    @Override
    public double calculatePnl(Position position, double returnShock) {
        double deltaS = PricingUtils.logReturnToAbsoluteShock(position.getSpotPrice(), returnShock);
        double deltaTerm = position.getDelta() * deltaS;
        double gammaTerm = 0.5 * position.getGamma() * deltaS * deltaS;

        return position.getQuantity() * (deltaTerm + gammaTerm);
    }
}

