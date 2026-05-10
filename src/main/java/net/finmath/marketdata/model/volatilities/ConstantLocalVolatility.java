package net.finmath.marketdata.model.volatilities;

/**
 * Constant local volatility, representing the classical Black-Scholes case.
 *
 * @author Alessandro Gnoatto
 */
public class ConstantLocalVolatility implements LocalVolatility {

	/**
	 * The volatility.
	 */
	private final double volatility;

	/**
	 * Creates a constant local volatility.
	 *
	 * @param volatility The volatility.
	 */
	public ConstantLocalVolatility(final double volatility) {
		if (volatility < 0.0 || Double.isNaN(volatility) || Double.isInfinite(volatility)) {
			throw new IllegalArgumentException("Volatility must be non-negative and finite.");
		}

		this.volatility = volatility;
	}

	@Override
	public double getValue(final double time, final double assetValue) {
		return volatility;
	}

	/**
	 * Returns the constant volatility.
	 *
	 * @return The volatility.
	 */
	public double getVolatility() {
		return volatility;
	}
}
