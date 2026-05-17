package net.finmath.finitedifference.solvers;

/**
 * Provides the outer boundary values for the active regime in a two-state
 * knock-in finite difference solver.
 * <p>
 * The active regime represents the post-activation value, that is, the value
 * of the corresponding vanilla contract after the barrier event has occurred.
 * Implementations of this interface define the boundary conditions at the
 * lower and upper ends of the spatial grid for that active regime.
 * <p>
 * The boundary values depend on the evaluation time and on the corresponding
 * boundary state variable.
 *
 * @author Alessandro Gnoatto
 */
public interface TwoStateActiveBoundaryProvider {

	/**
	 * Returns the boundary value at the lower spatial boundary for the active
	 * regime.
	 *
	 * @param time The evaluation time.
	 * @param stateVariable The state variable at the lower boundary.
	 * @return The boundary value at the lower boundary in the active regime.
	 */
	double getLowerBoundaryValue(double time, double stateVariable);

	/**
	 * Returns the boundary value at the upper spatial boundary for the active
	 * regime.
	 *
	 * @param time The evaluation time.
	 * @param stateVariable The state variable at the upper boundary.
	 * @return The boundary value at the upper boundary in the active regime.
	 */
	double getUpperBoundaryValue(double time, double stateVariable);
}
