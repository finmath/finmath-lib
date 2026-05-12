package net.finmath.finitedifference.boundaries;

/**
 * Boundary condition for one boundary component of a finite-difference problem.
 *
 * <p>
 * A boundary condition consists of a type and, if needed, a numerical value.
 * For {@link BoundaryConditionType#NONE}, the value is ignored.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface BoundaryCondition {

	/**
	 * Returns the type of boundary condition.
	 *
	 * @return The boundary condition type.
	 */
	BoundaryConditionType getType();

	/**
	 * Returns the boundary value.
	 *
	 * <p>
	 * This is used only when the type is {@link
	 * BoundaryConditionType#DIRICHLET}.
	 * </p>
	 *
	 * @return The boundary value.
	 */
	double getValue();

	/**
	 * Returns true if the condition is of Dirichlet type.
	 *
	 * @return True if Dirichlet.
	 */
	default boolean isDirichlet() {
		return getType() == BoundaryConditionType.DIRICHLET;
	}

	/**
	 * Returns true if no boundary row should be enforced.
	 *
	 * @return True if no boundary condition is imposed.
	 */
	default boolean isNone() {
		return getType() == BoundaryConditionType.NONE;
	}
}
