package domain.service.pricing;

/**
 * Utility functions for pricing and scenario shock translation.
 */
public final class PricingUtils {

    private PricingUtils() {
    }

    /**
     * Converts a log-return (r_t) into an absolute price difference (delta S).
     *
     * <p>Formula:
     * {@code delta S = S * (exp(r) - 1)}
     *
     * @param spotPrice   current underlying price (S)
     * @param logReturn   the simulated log-return (r)
     * @return absolute shock (delta S)
     */
    public static double logReturnToAbsoluteShock(double spotPrice, double logReturn) {
        return spotPrice * (Math.exp(logReturn) - 1.0);
    }
}

