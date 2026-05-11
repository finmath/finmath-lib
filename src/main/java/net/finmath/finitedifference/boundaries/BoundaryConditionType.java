package net.finmath.finitedifference.boundaries;

/**
 * Type of boundary condition used by finite-difference solvers.
 *
 * <p>
 * Minimal initial set:
 * </p>
 * <ul>
 * <li>{@link #DIRICHLET}: overwrite the corresponding PDE row with a fixed
 * value,</li>
 * <li>{@link #NONE}: do not enforce a boundary row; leave the PDE operator
 * intact.</li>
 * </ul>
 *
 * <p>
 * Additional types such as NEUMANN or ROBIN may be added later.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public enum BoundaryConditionType {
	/**
	 * The dirichlet.
	 */
	DIRICHLET,
	/**
	 * The none.
	 */
	NONE
}
