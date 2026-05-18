package net.finmath.finitedifference.solvers;

/**
 * Governs how barrier activation couples the inactive and active regimes in the
 * direct one-dimensional two-state knock-in solver.
 *
 * <p>
 * In the two-state formulation, the product value is decomposed into:
 * </p>
 * <ul>
 * <li>an inactive regime, representing paths for which the barrier has not yet
 * been hit,</li>
 *   <li>an active regime, representing paths after barrier activation.</li>
 * </ul>
 *
 * <p>
 * Let <i>V^{\mathrm{inact}}(t,x)</i> denote the inactive-regime value and
 * <i>V^{\mathrm{act}}(t,x)</i> the active-regime value, where <i>x</i> is the
 * state
 * variable. A {@code TwoStateActivationPolicy} specifies how the inactive
 * regime is
 * coupled to the active regime:
 * </p>
 * <ul>
 *   <li>on the already-hit region at maturity,</li>
 *   <li>on the already-hit region during backward propagation,</li>
 * <li>at the barrier interface, where the continuation-side inactive PDE sees a
 *       Dirichlet boundary value.</li>
 * </ul>
 *
 * <p>
 * Different policies correspond to different contractual semantics. For
 * example:
 * </p>
 * <ul>
 * <li>a continuation policy uses the active continuation value after
 * activation,</li>
 * <li>an immediate-cash policy replaces the post-hit value by a fixed cash
 * amount.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public interface TwoStateActivationPolicy {

	/**
	 * Returns the inactive-regime value on the already-hit region at maturity.
	 *
	 * @param stateVariable State variable.
	 * @param activePayoffValue Terminal payoff used for the active regime.
	 * @param inactiveNoHitValue Terminal value if the barrier has never been
	 *     hit.
	 * @return Inactive-regime terminal value on the already-hit region.
	 */
	double getAlreadyHitValueAtMaturity(
			double stateVariable,
			double activePayoffValue,
			double inactiveNoHitValue
			);

	/**
	 * Returns the inactive-regime value on the already-hit region during
	 * backward stepping.
	 *
	 * @param currentTime Current model time.
	 * @param stateVariable State variable.
	 * @param activeValue Value of the active regime at the same grid point.
	 * @return Inactive-regime value on the already-hit region.
	 */
	double getAlreadyHitValue(
			double currentTime,
			double stateVariable,
			double activeValue
			);

	/**
	 * Returns the interface value seen by the continuation-side inactive PDE at
	 * the barrier.
	 *
	 * @param currentTime Current model time.
	 * @param barrierStateVariable State variable at the barrier node.
	 * @param activeValueAtBarrier Active-regime value at the barrier node.
	 * @return Interface value for the inactive continuation-side solve.
	 */
	double getInterfaceValue(
			double currentTime,
			double barrierStateVariable,
			double activeValueAtBarrier
			);
}
