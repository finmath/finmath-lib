package net.finmath.marketdata.model.volatilities;

import java.util.Arrays;
import java.util.Objects;

import net.finmath.time.TimeDiscretization;

/**
 * Piecewise constant local volatility function.
 *
 * <p>
 * The volatility is a deterministic function of running time only. It does not
 * depend on the asset value. Hence it represents a time-dependent
 * Black-Scholes-type volatility as a {@link LocalVolatility}.
 * </p>
 *
 * <p>
 * The time discretization represents running times {@code t}, not time to
 * maturity. The convention is:
 * </p>
 * <ul>
 *     <li>{@code volatilities[i]} applies from
 *     {@code timeDiscretization.getTime(i)} inclusive to
 *     {@code timeDiscretization.getTime(i+1)} exclusive;</li>
 *     <li>the last volatility applies from the last discretization time
 *     onward;</li>
 * <li>before the first discretization time, the first volatility is used.</li>
 * </ul>
 *
 * <p>
 * The number of volatility values must equal
 * {@code timeDiscretization.getNumberOfTimes()}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class PiecewiseConstantVolatility implements LocalVolatility {

	/**
	 * The time discretization.
	 */
	private final TimeDiscretization timeDiscretization;
	/**
	 * The volatilities.
	 */
	private final double[] volatilities;

	/**
	 * Creates a piecewise constant volatility from a time discretization and an
	 * array of volatility values.
	 *
	 * @param timeDiscretization The running-time discretization at which the
	 *        volatility values become active.
	 * @param volatilities The volatility values.
	 */
	public PiecewiseConstantVolatility(
			final TimeDiscretization timeDiscretization,
			final double[] volatilities) {

		this.timeDiscretization = Objects.requireNonNull(
				timeDiscretization,
				"timeDiscretization"
				);

		if (volatilities == null || volatilities.length == 0) {
			throw new IllegalArgumentException("The volatility array must not be null or empty.");
		}

		if (volatilities.length != timeDiscretization.getNumberOfTimes()) {
			throw new IllegalArgumentException(
					"The number of volatility values must equal the number of "
							+ "time discretization points. Received "
							+ volatilities.length
							+ " volatility values and "
							+ timeDiscretization.getNumberOfTimes()
							+ " time points."
					);
		}

		this.volatilities = volatilities.clone();

		for (final double volatility : this.volatilities) {
			checkVolatility(volatility);
		}
	}

	/**
	 * Returns the local volatility {@code sigma(t,S)}.
	 *
	 * <p>
	 * Since this implementation is piecewise constant in time and independent
	 * of
	 * the asset value, the argument {@code assetValue} is ignored.
	 * </p>
	 *
	 * @param time The running time.
	 * @param assetValue The asset value.
	 * @return The piecewise constant volatility at the given time.
	 */
	@Override
	public double getValue(final double time, final double assetValue) {

		if (Double.isNaN(time) || Double.isInfinite(time)) {
			throw new IllegalArgumentException("The time must be finite.");
		}

		int timeIndex = timeDiscretization.getTimeIndexNearestLessOrEqual(time);

		if (timeIndex < 0) {
			timeIndex = 0;
		}

		if (timeIndex >= volatilities.length) {
			timeIndex = volatilities.length - 1;
		}

		return volatilities[timeIndex];
	}

	/**
	 * Returns the volatility value associated with a time index.
	 *
	 * @param timeIndex The time index.
	 * @return The volatility value.
	 */
	public double getVolatility(final int timeIndex) {

		if (timeIndex < 0 || timeIndex >= volatilities.length) {
			throw new IllegalArgumentException(
					"Time index out of bounds: " + timeIndex
					);
		}

		return volatilities[timeIndex];
	}

	/**
	 * Returns the volatility value active at the given running time.
	 *
	 * @param time The running time.
	 * @return The volatility value.
	 */
	public double getVolatility(final double time) {
		return getValue(time, 0.0);
	}

	/**
	 * Returns the running-time discretization.
	 *
	 * @return The running-time discretization.
	 */
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	/**
	 * Returns a defensive copy of the volatility array.
	 *
	 * @return The volatility array.
	 */
	public double[] getVolatilities() {
		return volatilities.clone();
	}

	/**
	 * Returns the number of volatility values.
	 *
	 * @return The number of volatility values.
	 */
	public int getNumberOfVolatilities() {
		return volatilities.length;
	}

	/**
	 * Creates a clone with modified volatility values and the same time
	 * discretization.
	 *
	 * @param newVolatilities The new volatility values.
	 * @return The modified volatility function.
	 */
	public PiecewiseConstantVolatility getCloneForModifiedVolatilities(
			final double[] newVolatilities) {

		return new PiecewiseConstantVolatility(
				timeDiscretization,
				newVolatilities
				);
	}

	/**
	 * Creates a clone with a modified time discretization and the same
	 * volatility
	 * values.
	 *
	 * @param newTimeDiscretization The new time discretization.
	 * @return The modified volatility function.
	 */
	public PiecewiseConstantVolatility getCloneForModifiedTimeDiscretization(
			final TimeDiscretization newTimeDiscretization) {

		return new PiecewiseConstantVolatility(
				newTimeDiscretization,
				volatilities
				);
	}

	private static void checkVolatility(final double volatility) {

		if (Double.isNaN(volatility) || Double.isInfinite(volatility)) {
			throw new IllegalArgumentException("Volatility must be finite.");
		}

		if (volatility < 0.0) {
			throw new IllegalArgumentException("Volatility must be non-negative.");
		}
	}

	@Override
	public String toString() {
		return "PiecewiseConstantVolatility [timeDiscretization="
				+ timeDiscretization
				+ ", volatilities="
				+ Arrays.toString(volatilities)
				+ "]";
	}
}
