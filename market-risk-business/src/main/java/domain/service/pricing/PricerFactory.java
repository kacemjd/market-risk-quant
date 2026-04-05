package domain.service.pricing;

import domain.model.Position;

/**
 * Factory linking Instruments to their corresponding Pricer strategy.
 */
public class PricerFactory {

    private static final Pricer LINEAR = new LinearPricer();
    private static final Pricer DELTA_GAMMA = new DeltaGammaPricer();

    private PricerFactory() {}

    /**
     * Determine best pricing strategy per instrument. Over-simplified logic here
     * checks for non-zero Gamma to route to DeltaGammaPricer.
     */
    public static Pricer getPricerFor(Position position) {
        if (position.getGamma() != 0.0) {
            return DELTA_GAMMA;
        }
        return LINEAR;
    }
}

