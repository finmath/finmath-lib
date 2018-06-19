package net.finmath.finitedifference.products;

/**
 * Interface for boundaries conditions provided to one dimensional finite difference solvers.
 * 
 * @author Christian Fries
 */
public interface FiniteDifference1DBoundary {

	/**
	 * Return the value of the value process at the lower boundary for a given time and asset value.
	 * 
	 * @param time The time at which the boundary is observed.
	 * @param assetValue The value of the asset specifying the location of the boundary.
	 * @return
	 */
	double getValueAtLowerBoundary(double time, double assetValue);

	/**
	 * Return the value of the value process at the upper boundary for a given time and asset value.
	 * 
	 * @param time The time at which the boundary is observed.
	 * @param assetValue The value of the asset specifying the location of the boundary.
	 * @return
	 */
	double getValueAtUpperBoundary(double time, double assetValue);
	
}