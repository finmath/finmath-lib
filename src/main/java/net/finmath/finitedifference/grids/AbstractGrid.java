package net.finmath.finitedifference.grids;

import java.util.Arrays;

/**
 * Abstract base class for implementations of {@link Grid}.
 *
 * <p>
 * Provides a default implementation of {@link #getInteriorGrid()},
 * returning the grid without the first and last points.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public abstract class AbstractGrid implements Grid {

	/**
	 * Returns the interior grid, excluding the first and the last entry.
	 *
	 * @return An array containing the interior grid points.
	 */
	@Override
	public double[] getInteriorGrid() {
		final double[] fullGrid = getGrid();
		return Arrays.copyOfRange(fullGrid, 1, fullGrid.length - 1);
	}
}
