package net.finmath.finitedifference.grids;

/**
 * Interface representing a spatial grid used in finite difference methods.
 *
 * <p>
 * Provides access to the grid points and their spacing (deltas), supporting
 * both uniform and non-uniform grids.
 * </p>
 *
 * @author Michela Birtele
 */
public interface Grid {

	/**
	 * Returns the array of grid points representing the spatial locations.
	 *
	 * @return An array of grid points.
	 */
	double[] getGrid();

	/**
	 * Calculates the spacing (delta) between consecutive grid points
	 * based on the provided values on the grid.
	 *
	 * @param valuesOnGrid An array of values defined at the grid points.
	 * @return An array of deltas representing the spacing between points.
	 */
	double[] getDelta(double[] valuesOnGrid);

	/**
	 * Returns the interior grid, excluding the first and the last entry.
	 *
	 * @return An array containing the interior grid points.
	 */
	double[] getInteriorGrid();
}
