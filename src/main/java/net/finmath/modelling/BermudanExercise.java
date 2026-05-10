package net.finmath.modelling;

import java.time.LocalDate;
import java.util.Arrays;

import net.finmath.time.FloatingpointDate;

/**
 * Bermudan exercise: exercise allowed only on a finite set of dates.
 *
 * @author Alessandro Gnoatto
 */
public class BermudanExercise extends AbstractExercise {

	/**
	 * Creates a Bermudan exercise from running times.
	 *
	 * @param exerciseTimes The exercise times in running time.
	 */
	public BermudanExercise(final double[] exerciseTimes) {
		super(getLastExerciseTime(exerciseTimes), exerciseTimes);
	}

	/**
	 * Creates a Bermudan exercise from dates.
	 *
	 * @param referenceDate The reference date.
	 * @param exerciseDates The exercise dates.
	 */
	public BermudanExercise(final LocalDate referenceDate,
			final LocalDate[] exerciseDates) {
		this(convertDates(referenceDate, exerciseDates));
	}

	private static double getLastExerciseTime(final double[] exerciseTimes) {
		if (exerciseTimes == null || exerciseTimes.length == 0) {
			throw new IllegalArgumentException("Exercise times must not be null or empty.");
		}
		final double[] sorted = exerciseTimes.clone();
		Arrays.sort(sorted);
		return sorted[sorted.length - 1];
	}

	private static double[] convertDates(final LocalDate referenceDate, final LocalDate[] exerciseDates) {
		if (exerciseDates == null || exerciseDates.length == 0) {
			throw new IllegalArgumentException("Exercise dates must not be null or empty.");
		}

		final double[] exerciseTimes = new double[exerciseDates.length];
		for (int i = 0; i < exerciseDates.length; i++) {
			exerciseTimes[i] = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, exerciseDates[i]);
		}
		return exerciseTimes;
	}

	@Override
	public boolean isContinuousExercise() {
		return false;
	}

	@Override
	public boolean isExerciseAllowed(final double time) {
		return isScheduledExerciseTime(time);
	}

	@Override
	public boolean isEuropean() {
		return false;
	}

	@Override
	public boolean isAmerican() {
		return false;
	}

	@Override
	public boolean isBermudan() {
		return true;
	}
}
