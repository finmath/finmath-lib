package net.finmath.finitedifference;

import java.util.Set;
import java.util.TreeSet;

import net.finmath.modelling.AmericanExercise;
import net.finmath.modelling.Exercise;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Utility methods adapting a general {@link Exercise} specification to
 * finite-difference solvers working in time-to-maturity coordinates.
 *
 * <p>
 * Finite-difference solvers in this project use time-to-maturity
 * {@code tau = maturity - t}. Exercise specifications are given in running time
 * {@code t}. This class performs the required conversion and can refine the
 * time discretization so that all Bermudan exercise dates are represented
 * exactly.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class FiniteDifferenceExerciseUtil {

	/**
	 * The time tolerance.
	 */
	private static final double TIME_TOLERANCE = 1E-12;

	private FiniteDifferenceExerciseUtil() {
	}

	/**
	 * Returns the exercise times in time-to-maturity coordinates.
	 *
	 * @param exercise The exercise specification.
	 * @return The exercise times in tau = maturity - t.
	 */
	public static double[] getExerciseTimesToMaturity(final Exercise exercise) {
		final double maturity = exercise.getMaturity();
		final double[] exerciseTimes = exercise.getExerciseTimes();
		final double[] tauTimes = new double[exerciseTimes.length];

		for (int i = 0; i < exerciseTimes.length; i++) {
			tauTimes[i] = maturity - exerciseTimes[i];
		}

		java.util.Arrays.sort(tauTimes);
		return tauTimes;
	}

	/**
	 * Returns a refined time discretization that includes all Bermudan exercise
	 * dates
	 * in time-to-maturity coordinates.
	 *
	 * <p>
	 * European and American exercise return the base discretization unchanged.
	 * </p>
	 *
	 * @param baseTimeDiscretization The original time discretization in tau.
	 * @param exercise The exercise specification.
	 * @return A refined time discretization in tau.
	 */
	public static TimeDiscretization refineTimeDiscretization(
			final TimeDiscretization baseTimeDiscretization,
			final Exercise exercise) {

		if (exercise.isEuropean() || exercise.isAmerican()) {
			return baseTimeDiscretization;
		}

		final Set<Double> mergedTimes = new TreeSet<>();

		for (int i = 0; i < baseTimeDiscretization.getNumberOfTimes(); i++) {
			mergedTimes.add(baseTimeDiscretization.getTime(i));
		}

		for (final double tau : getExerciseTimesToMaturity(exercise)) {
			mergedTimes.add(tau);
		}

		final double[] refinedTimes = new double[mergedTimes.size()];
		int index = 0;
		for (final Double time : mergedTimes) {
			refinedTimes[index++] = time;
		}

		return new TimeDiscretizationFromArray(refinedTimes);
	}

	/**
	 * Returns true if exercise is allowed at the given time-to-maturity.
	 *
	 * @param tau The time-to-maturity.
	 * @param exercise The exercise specification.
	 * @return True if exercise is allowed.
	 */
	public static boolean isExerciseAllowedAtTimeToMaturity(final double tau, final Exercise exercise) {
		final double runningTime = exercise.getMaturity() - tau;
		return exercise.isExerciseAllowed(runningTime);
	}

	/**
	 * Returns true if the exercise policy should be treated as continuous on
	 * the FD grid.
	 *
	 * @param exercise The exercise specification.
	 * @return True if the policy is continuous.
	 */
	public static boolean isContinuousExercise(final Exercise exercise) {
		return exercise.isContinuousExercise();
	}

	/**
	 * Returns true if the given tau-grid time is an actual Bermudan exercise
	 * time.
	 *
	 * @param tau The time-to-maturity.
	 * @param exercise The exercise specification.
	 * @return True if tau corresponds to a Bermudan exercise date.
	 */
	public static boolean isDiscreteExerciseTime(final double tau, final Exercise exercise) {
		if (exercise.isAmerican()) {
			return false;
		}

		for (final double exerciseTau : getExerciseTimesToMaturity(exercise)) {
			if (Math.abs(exerciseTau - tau) < TIME_TOLERANCE) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Convenience method returning the exercise start time in tau-coordinates
	 * for American exercise.
	 *
	 * @param exercise The exercise specification.
	 * @return The corresponding tau value, or 0.0 if not applicable.
	 */
	public static double getAmericanExerciseStartTimeToMaturity(final Exercise exercise) {
		if (!(exercise instanceof AmericanExercise)) {
			return 0.0;
		}

		final AmericanExercise americanExercise = (AmericanExercise) exercise;
		return exercise.getMaturity() - americanExercise.getExerciseStartTime();
	}
}
