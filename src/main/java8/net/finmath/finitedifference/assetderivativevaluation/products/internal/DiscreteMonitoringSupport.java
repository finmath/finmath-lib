package net.finmath.finitedifference.assetderivativevaluation.products.internal;

import java.util.Set;
import java.util.TreeSet;

import net.finmath.modelling.products.MonitoringType;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Utility methods for products with continuous or discrete monitoring.
 *
 * <p>
 * This class centralizes common monitoring-related functionality used by
 * finite-difference products. It provides helpers to identify discrete
 * monitoring, test whether a time belongs to a monitoring schedule, validate
 * monitoring inputs, and refine a solver time discretization so that monitoring
 * dates are represented exactly as backward-time grid points.
 * </p>
 *
 * <p>
 * The finite-difference solvers use the backward time variable
 * {@code tau = maturity - time}. Consequently, when monitoring dates are merged
 * into a solver time grid, the inserted grid points are {@code maturity -
 * monitoringTime}.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class DiscreteMonitoringSupport {

	/**
	 * Default absolute tolerance used when comparing monitoring times.
	 */
	public static final double DEFAULT_MONITORING_TIME_TOLERANCE = 1E-12;

	private DiscreteMonitoringSupport() {
	}

	/**
	 * Returns whether the given monitoring type represents discrete monitoring.
	 *
	 * @param monitoringType The monitoring type.
	 * @return {@code true} if {@code monitoringType} is
	 *         {@link MonitoringType#DISCRETE}; {@code false} otherwise.
	 */
	public static boolean usesDiscreteMonitoring(final MonitoringType monitoringType) {
		return monitoringType == MonitoringType.DISCRETE;
	}

	/**
	 * Tests whether a time belongs to a discrete monitoring schedule.
	 *
	 * <p>
	 * A time is considered a monitoring time if it differs from one of the
	 * supplied monitoring times by at most the supplied absolute tolerance.
	 * </p>
	 *
	 * @param time The time to test.
	 * @param monitoringTimes The monitoring schedule.
	 * @param tolerance The absolute tolerance used for time comparison.
	 * @return {@code true} if {@code time} matches one of the monitoring times;
	 *         {@code false} otherwise.
	 */
	public static boolean isMonitoringTime(
			final double time,
			final double[] monitoringTimes,
			final double tolerance) {

		if (monitoringTimes == null) {
			return false;
		}

		for (final double monitoringTime : monitoringTimes) {
			if (Math.abs(monitoringTime - time) <= tolerance) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Validates a monitoring specification.
	 *
	 * <p>
	 * Continuous monitoring must not provide monitoring times. Discrete
	 * monitoring must provide a non-empty, strictly increasing array of
	 * monitoring times lying in the interval {@code [0, maturity]}, up to the
	 * supplied tolerance.
	 * </p>
	 *
	 * @param monitoringType The monitoring type.
	 * @param monitoringTimes The monitoring schedule.
	 * @param maturity The product maturity.
	 * @param tolerance The absolute tolerance used for validation.
	 * @throws IllegalArgumentException Thrown if the monitoring type is
	 * {@code null}, if continuous monitoring specifies monitoring times,
	 *         or if discrete monitoring has missing, unordered, or out-of-range
	 *         monitoring times.
	 */
	public static void validateMonitoringSpecification(
			final MonitoringType monitoringType,
			final double[] monitoringTimes,
			final double maturity,
			final double tolerance) {

		if (monitoringType == null) {
			throw new IllegalArgumentException("monitoringType must not be null.");
		}

		if (monitoringType == MonitoringType.CONTINUOUS) {
			if (monitoringTimes != null && monitoringTimes.length > 0) {
				throw new IllegalArgumentException(
						"Continuous monitoring must not specify monitoringTimes."
				);
			}
			return;
		}

		if (monitoringTimes == null || monitoringTimes.length == 0) {
			throw new IllegalArgumentException(
					"Discrete monitoring requires a non-empty monitoringTimes array."
			);
		}

		double previousTime = -Double.MAX_VALUE;
		for (final double monitoringTime : monitoringTimes) {
			if (monitoringTime < -tolerance || monitoringTime > maturity + tolerance) {
				throw new IllegalArgumentException(
						"Monitoring times must lie in [0,maturity]."
				);
			}

			if (monitoringTime <= previousTime + tolerance) {
				throw new IllegalArgumentException(
						"Monitoring times must be strictly increasing."
				);
			}

			previousTime = monitoringTime;
		}
	}

	/**
	 * Refines a backward-time discretization with monitoring dates.
	 *
	 * <p>
	 * The returned time discretization contains all original times from
	 * {@code baseTimeDiscretization} and, for each monitoring date
	 * {@code t_i}, the corresponding backward-time point
	 * {@code maturity - t_i}. Duplicate points are removed by the
	 * {@link TreeSet} merge.
	 * </p>
	 *
	 * @param baseTimeDiscretization The original solver time discretization.
	 * @param maturity The product maturity.
	 * @param monitoringTimes The monitoring schedule.
	 * @return A refined time discretization containing the original grid and
	 *     the
	 *         monitoring dates converted to backward time, or the original
	 * discretization if the monitoring schedule is {@code null} or empty.
	 */
	public static TimeDiscretization refineTimeDiscretizationWithMonitoring(
			final TimeDiscretization baseTimeDiscretization,
			final double maturity,
			final double[] monitoringTimes) {

		if (monitoringTimes == null || monitoringTimes.length == 0) {
			return baseTimeDiscretization;
		}

		final Set<Double> mergedTauTimes = new TreeSet<>();

		for (int i = 0; i < baseTimeDiscretization.getNumberOfTimes(); i++) {
			mergedTauTimes.add(baseTimeDiscretization.getTime(i));
		}

		for (final double monitoringTime : monitoringTimes) {
			mergedTauTimes.add(maturity - monitoringTime);
		}

		final double[] refinedTimes = new double[mergedTauTimes.size()];
		int index = 0;
		for (final Double time : mergedTauTimes) {
			refinedTimes[index++] = time;
		}

		return new TimeDiscretizationFromArray(refinedTimes);
	}
}
