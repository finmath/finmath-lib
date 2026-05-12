package net.finmath.finitedifference.assetderivativevaluation.products;

/**
 * Enumeration of supported exercise styles for finite-difference products.
 *
 * <p>
 * The exercise type determines the set of times at which the holder may
 * exercise the option:
 * </p>
 * <ul>
 *   <li>{@link #EUROPEAN}: exercise is allowed only at maturity <i>T</i>,</li>
 * <li>{@link #BERMUDAN}: exercise is allowed only on a prescribed discrete set
 * of exercise dates,</li>
 * <li>{@link #AMERICAN}: exercise is allowed at any time in a continuous
 * interval up to maturity.</li>
 * </ul>
 *
 * <p>
 * In optimal stopping terms, if <i>V(t,S)</i> denotes the option value and
 * <i>G(t,S)</i> the immediate exercise value, then:
 * </p>
 * <ul>
 *   <li>for European exercise, the stopping time is fixed to maturity,</li>
 * <li>for Bermudan exercise, stopping is restricted to a discrete set of
 * dates,</li>
 *   <li>for American exercise, the value satisfies the obstacle relation
 * <i>V(t,S) = max(G(t,S), C(t,S))</i>, where <i>C</i> denotes the continuation
 * value.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public enum ExerciseType {
	/**
	 * The european.
	 */
	EUROPEAN,
	/**
	 * The bermudan.
	 */
	BERMUDAN,
	/**
	 * The american.
	 */
	AMERICAN;
}
