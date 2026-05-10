package net.finmath.modelling;

import java.util.Arrays;

/**
 * Base class for exercise specifications.
 *
 * @author Alessandro Gnoatto
 */
public abstract class AbstractExercise implements Exercise {

    /**
     * The time tolerance.
     */
    protected static final double TIME_TOLERANCE = 1E-12;

    /**
     * The maturity.
     */
    private final double maturity;
    /**
     * The exercise times.
     */
    private final double[] exerciseTimes;

    /**
     * Creates an exercise specification.
     *
     * @param maturity The maturity.
     * @param exerciseTimes The exercise times in running time.
     */
    protected AbstractExercise(final double maturity, final double[] exerciseTimes) {
        if (maturity < 0.0) {
            throw new IllegalArgumentException("Maturity must be non-negative.");
        }
        if (exerciseTimes == null || exerciseTimes.length == 0) {
            throw new IllegalArgumentException("Exercise times must not be null or empty.");
        }

        this.maturity = maturity;
        this.exerciseTimes = exerciseTimes.clone();
        Arrays.sort(this.exerciseTimes);

        for (final double exerciseTime : this.exerciseTimes) {
            if (exerciseTime < -TIME_TOLERANCE || exerciseTime > maturity + TIME_TOLERANCE) {
                throw new IllegalArgumentException("Exercise time out of range: " + exerciseTime);
            }
        }
    }

    @Override
    public double getMaturity() {
        return maturity;
    }

    @Override
    public double[] getExerciseTimes() {
        return exerciseTimes.clone();
    }

    /**
     * Helper to test whether a time coincides with one of the scheduled
     * exercise times.
     *
     * @param time The running time.
     * @return True if the time matches one of the scheduled exercise times.
     */
    protected boolean isScheduledExerciseTime(final double time) {
        for (final double exerciseTime : exerciseTimes) {
            if (Math.abs(exerciseTime - time) < TIME_TOLERANCE) {
                return true;
            }
        }
        return false;
    }
}
