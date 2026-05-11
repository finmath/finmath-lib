package net.finmath.finitedifference.assetderivativevaluation.boundaries;

import net.finmath.finitedifference.assetderivativevaluation.products.FiniteDifferenceEquityProduct;
import net.finmath.finitedifference.boundaries.BoundaryCondition;

/**
 * Interface for boundaries conditions provided to finite difference solvers.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface FiniteDifferenceBoundary {

	/**
	 * Returns the boundary conditions at the lower boundary.
	 *
	 * <p>
	 * The returned array is indexed by state-variable dimension.
	 * </p>
	 *
	 * @param product The product being valued.
	 * @param time The running time.
	 * @param stateVariables The state variables specifying the boundary
	 *     location.
	 * @return The lower-boundary conditions by dimension.
	 */
	BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			FiniteDifferenceEquityProduct product,
			double time,
			double... stateVariables);

	/**
	 * Returns the boundary conditions at the upper boundary.
	 *
	 * <p>
	 * The returned array is indexed by state-variable dimension.
	 * </p>
	 *
	 * @param product The product being valued.
	 * @param time The running time.
	 * @param stateVariables The state variables specifying the boundary
	 *     location.
	 * @return The upper-boundary conditions by dimension.
	 */
	BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			FiniteDifferenceEquityProduct product,
			double time,
			double... stateVariables);
}
