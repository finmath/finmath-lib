package net.finmath.finitedifference.models;

/**
 * Interface for boundaries conditions provided to one dimensional finite difference solvers.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface FiniteDifference1DBoundary {

	/**
	 * Return the value of the value process at the lower boundary for a given time and asset value.
	 * @param model The model which uses the boundary condition (provides model parameters)
	 * @param time The time at which the boundary is observed.
	 * @param assetValue The value of the asset specifying the location of the boundary.
	 *
	 * @return the value process at the lower boundary
	 */
	double getValueAtLowerBoundary(FiniteDifference1DModel model, double time, double assetValue);

	/**
	 * Return the value of the value process at the upper boundary for a given time and asset value.
	 * @param model TODO
	 * @param time The time at which the boundary is observed.
	 * @param assetValue The value of the asset specifying the location of the boundary.
	 *
	 * @return the value process at the upper boundary
	 */
	double getValueAtUpperBoundary(FiniteDifference1DModel model, double time, double assetValue);

}
