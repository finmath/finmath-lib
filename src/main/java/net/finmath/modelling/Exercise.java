package net.finmath.modelling;

/**
 * General exercise specification for an option-like product.
 *
 * <p>
 * Exercise times are expressed in running time, i.e. increasing calendar-time
 * coordinates
 * measured from the model reference date.
 * </p>
 *
 * <p>
 * The interface is intentionally method-agnostic and may be used by finite-
 * difference,
 * Monte Carlo, lattice / tree, or other numerical methods.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface Exercise {

    /**
     * Returns the maturity of the contract in running time.
     *
     * @return The maturity.
     */
    double getMaturity();

    /**
     * Returns the exercise times in running time.
     *
     * <p>
     * For European exercise this is typically a singleton containing only the
     * maturity.
     * For American exercise this may also return only the maturity, since the
     * exercise
     * region is continuous and not represented by a finite list.
     * For Bermudan exercise this returns the scheduled exercise dates.
     * </p>
     *
     * @return The exercise times.
     */
    double[] getExerciseTimes();

    /**
     * Returns whether exercise is allowed continuously in time.
     *
     * @return True if the exercise right is continuous in time.
     */
    boolean isContinuousExercise();

    /**
     * Returns whether exercise is allowed at the given running time.
     *
     * <p>
     * This method is interpreted in contractual terms, not in numerical-grid
     * terms.
     * For Bermudan exercise it checks whether the given time matches one of the
     * scheduled
     * exercise times (up to a small tolerance). For American exercise it checks
     * whether
     * the time lies inside the exercise interval.
     * </p>
     *
     * @param time The running time.
     * @return True if exercise is allowed.
     */
    boolean isExerciseAllowed(double time);

    /**
     * Returns true if this is European exercise.
     *
     * @return True if European.
     */
    boolean isEuropean();

    /**
     * Returns true if this is American exercise.
     *
     * @return True if American.
     */
    boolean isAmerican();

    /**
     * Returns true if this is Bermudan exercise.
     *
     * @return True if Bermudan.
     */
    boolean isBermudan();
}
