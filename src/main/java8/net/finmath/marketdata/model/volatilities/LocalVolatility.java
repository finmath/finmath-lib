package net.finmath.marketdata.model.volatilities;

import net.finmath.stochastic.RandomVariable;

/**
 * Interface for a local volatility function of the form
 * {@code sigma = sigma(t, S)}.
 *
 * <p>
 * The interface is intentionally framework-neutral. The scalar method
 * {@link #getValue(double, double)} is the primary method and is suitable for
 * finite-difference models. The random-variable overload is provided as a
 * default method for Monte Carlo models.
 * </p>
 *
 * <p>
 * Implementations may represent local volatility obtained from Dupire,
 * Gatheral's implied-volatility formula, piecewise-constant Black-Scholes
 * volatility, deterministic local-volatility tables, or any other model.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface LocalVolatility {

	/**
	 * Returns the local volatility {@code sigma(t, S)}.
	 *
	 * @param time The evaluation time.
	 * @param assetValue The asset value.
	 * @return The local volatility.
	 */
	double getValue(double time, double assetValue);

	/**
	 * Returns the local volatility {@code sigma(t, S)}.
	 *
	 * @param time The evaluation time.
	 * @param assetValue The asset value.
	 * @return The local volatility.
	 */
	default double getLocalVolatility(final double time, final double assetValue) {
		return getValue(time, assetValue);
	}

	/**
	 * Returns the local volatility path-wise for a random variable representing
	 * the asset value.
	 *
	 * @param time The evaluation time.
	 * @param assetValue The asset value random variable.
	 * @return The local volatility random variable.
	 */
	default RandomVariable getValue(final double time, final RandomVariable assetValue) {
		return assetValue.apply(x -> getValue(time, x));
	}

	/**
	 * Returns the local volatility path-wise for a random variable representing
	 * the asset value.
	 *
	 * @param time The evaluation time.
	 * @param assetValue The asset value random variable.
	 * @return The local volatility random variable.
	 */
	default RandomVariable getLocalVolatility(final double time, final RandomVariable assetValue) {
		return getValue(time, assetValue);
	}
}
