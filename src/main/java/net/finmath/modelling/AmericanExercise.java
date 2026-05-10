package net.finmath.modelling;

import java.time.LocalDate;

import net.finmath.time.FloatingpointDate;

/**
 * American exercise: continuous exercise on an interval.
 *
 * <p>
 * The standard case is exercise from time 0 to maturity, but a later exercise
 * start time
 * can also be specified.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class AmericanExercise extends AbstractExercise {

    /**
     * The exercise start time.
     */
    private final double exerciseStartTime;

    /**
     * Creates a standard American exercise from time 0 to maturity.
     *
     * @param maturity The maturity.
     */
    public AmericanExercise(final double maturity) {
        this(0.0, maturity);
    }

    /**
     * Creates an American exercise on the interval [exerciseStartTime,
     * maturity].
     *
     * @param exerciseStartTime The first time at which exercise is allowed.
     * @param maturity The maturity.
     */
    public AmericanExercise(final double exerciseStartTime, final double maturity) {
        super(maturity, new double[] {maturity });

        if (exerciseStartTime < 0.0 || exerciseStartTime > maturity) {
            throw new IllegalArgumentException("Invalid American exercise interval.");
        }

        this.exerciseStartTime = exerciseStartTime;
    }

    /**
     * Creates a standard American exercise from dates.
     *
     * @param referenceDate The reference date.
     * @param maturityDate The maturity date.
     */
    public AmericanExercise(final LocalDate referenceDate, final LocalDate maturityDate) {
        this(0.0, FloatingpointDate.getFloatingPointDateFromDate(referenceDate, maturityDate));
    }

    /**
     * Creates an American exercise window from dates.
     *
     * @param referenceDate The reference date.
     * @param exerciseStartDate The first exercise date.
     * @param maturityDate The maturity date.
     */
    public AmericanExercise(
            final LocalDate referenceDate,
            final LocalDate exerciseStartDate,
            final LocalDate maturityDate) {
        this(
                FloatingpointDate.getFloatingPointDateFromDate(referenceDate, exerciseStartDate),
                FloatingpointDate.getFloatingPointDateFromDate(referenceDate, maturityDate)
        );
    }

    /**
     * Returns the first time at which exercise is allowed.
     *
     * @return The exercise start time.
     */
    public double getExerciseStartTime() {
        return exerciseStartTime;
    }

    @Override
    public boolean isContinuousExercise() {
        return true;
    }

    @Override
    public boolean isExerciseAllowed(final double time) {
        return time >= exerciseStartTime - TIME_TOLERANCE
                && time <= getMaturity() + TIME_TOLERANCE;
    }

    @Override
    public boolean isEuropean() {
        return false;
    }

    @Override
    public boolean isAmerican() {
        return true;
    }

    @Override
    public boolean isBermudan() {
        return false;
    }
}
