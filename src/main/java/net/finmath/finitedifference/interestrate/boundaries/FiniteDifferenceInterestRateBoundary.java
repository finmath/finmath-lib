package net.finmath.finitedifference.interestrate.boundaries;

import net.finmath.finitedifference.boundaries.BoundaryCondition;
import net.finmath.finitedifference.interestrate.products.FiniteDifferenceInterestRateProduct;

/**
 * Interface for boundary conditions provided to finite-difference interest-rate
 * solvers.
 *
 * <p>
 * The returned array is indexed by state-variable dimension.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public interface FiniteDifferenceInterestRateBoundary {

	/**
	 * Returns the boundary conditions at the lower boundary.
	 *
	 * @param product The product being valued.
	 * @param time The running time.
	 * @param stateVariables The state variables specifying the boundary
	 *     location.
	 * @return The lower-boundary conditions by dimension.
	 */
	BoundaryCondition[] getBoundaryConditionsAtLowerBoundary(
			FiniteDifferenceInterestRateProduct product,
			double time,
			double... stateVariables);

	/**
	 * Returns the boundary conditions at the upper boundary.
	 *
	 * @param product The product being valued.
	 * @param time The running time.
	 * @param stateVariables The state variables specifying the boundary
	 *     location.
	 * @return The upper-boundary conditions by dimension.
	 */
	BoundaryCondition[] getBoundaryConditionsAtUpperBoundary(
			FiniteDifferenceInterestRateProduct product,
			double time,
			double... stateVariables);
}
