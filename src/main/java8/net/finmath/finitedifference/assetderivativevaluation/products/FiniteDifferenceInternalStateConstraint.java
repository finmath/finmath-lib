package net.finmath.finitedifference.assetderivativevaluation.products;

/**
 * Optional product-side internal state constraint for finite-difference
 * solvers.
 *
 * <p>
 * This interface is intended for products whose value is constrained not only
 * at
 * the outer PDE boundaries, but also on an internal subset of the state space.
 * Typical examples are barrier options, where nodes beyond the barrier are
 * knocked out and must be assigned a prescribed value.
 * </p>
 *
 * <p>
 * The solver may query this interface at each grid node and time step:
 * </p>
 * <ul>
 * <li>{@link #isConstraintActive(double, double...)} tells whether the node is
 * constrained,</li>
 * <li>{@link #getConstrainedValue(double, double...)} provides the value to
 * impose there.</li>
 * </ul>
 *
 * <p>
 * Time is given in running time, consistently with the boundary framework.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceInternalStateConstraint {

	/**
	 * Returns true if the internal constraint is active at the given node and
	 * time.
	 *
	 * @param time Running time.
	 * @param stateVariables State variables at the grid node.
	 * @return True if the node is constrained.
	 */
	boolean isConstraintActive(double time, double... stateVariables);

	/**
	 * Returns the value to impose at a constrained node.
	 *
	 * <p>
	 * This method is called only if {@link #isConstraintActive(double,
	 * double...)}
	 * returns true.
	 * </p>
	 *
	 * @param time Running time.
	 * @param stateVariables State variables at the grid node.
	 * @return The constrained value.
	 */
	double getConstrainedValue(double time, double... stateVariables);
}
