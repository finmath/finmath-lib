package net.finmath.finitedifference.grids;

import java.util.HashMap;

import net.finmath.time.TimeDiscretization;

/**
 * Encapsulates the space-time discretization used in a finite difference
 * scheme.
 *
 * <p>
 * The class contains:
 * </p>
 * <ul>
 *   <li>A possibly multi-dimensional spatial grid, stored as a collection of
 *       {@link Grid} objects indexed by their dimension (index {@code 0}
 *       corresponds to the one-dimensional case).</li>
 *   <li>A {@link TimeDiscretization}, interpreted as a discretization of
 *       time-to-maturity. In the finite difference framework, the PDE is
 *       solved backward in time, so {@code 0} corresponds to maturity.</li>
 *   <li>The parameter {@code theta}, controlling the theta-method
 *       (explicit, implicit, or Crank–Nicolson).</li>
 *   <li>A center point per spatial dimension.</li>
 * </ul>
 *
 * @author Alessandro Gnoatto
 */
public class SpaceTimeDiscretization {

	/**
	 * The space grids.
	 */
	private final HashMap<Integer, Grid> spaceGrids = new HashMap<>();
	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public HashMap<Integer, Grid> getSpaceGrids() {
		return spaceGrids;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double[] getCenter() {
		return center;
	}

	/**
	 * The time discretization.
	 */
	private final TimeDiscretization timeDiscretization;
	/**
	 * The theta.
	 */
	private final double theta;
	/**
	 * The center.
	 */
	private final double[] center;

	/**
	 * Constructs a one-dimensional space-time discretization.
	 *
	 * @param spaceGrid         The spatial grid.
	 * @param timeDiscretization The time discretization (time-to-maturity).
	 * @param theta             The theta parameter of the theta-method.
	 * @param center            The center point of the spatial grid.
	 */
	public SpaceTimeDiscretization(
			final Grid spaceGrid,
			final TimeDiscretization timeDiscretization,
			final double theta,
			final double[] center) {

		spaceGrids.put(0, spaceGrid);
		this.timeDiscretization = timeDiscretization;
		this.theta = theta;
		this.center = center;
	}

	/**
	 * Constructs a multi-dimensional space-time discretization.
	 *
	 * <p>
	 * The {@code i}-th grid in the array is stored under key {@code i}.
	 * </p>
	 *
	 * @param spaceGrids         Array of grids, one per spatial dimension.
	 * @param timeDiscretization The time discretization (time-to-maturity).
	 * @param theta              The theta parameter of the theta-method.
	 * @param center Center point per dimension (same length as {@code
	 *     spaceGrids}).
	 */
	public SpaceTimeDiscretization(
			final Grid[] spaceGrids,
			final TimeDiscretization timeDiscretization,
			final double theta,
			final double[] center) {

		for (int i = 0; i < spaceGrids.length; i++) {
			this.spaceGrids.put(i, spaceGrids[i]);
		}

		this.timeDiscretization = timeDiscretization;
		this.theta = theta;
		this.center = center;
	}

	/**
	 * Returns the spatial grid corresponding to the given dimension.
	 *
	 * @param dimension The spatial dimension index.
	 * @return The corresponding {@link Grid}.
	 */
	public Grid getSpaceGrid(final int dimension) {
		return spaceGrids.get(dimension);
	}

	/**
	 * Returns the time discretization.
	 *
	 * @return The {@link TimeDiscretization}.
	 */
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	/**
	 * Returns the theta parameter of the theta-method.
	 *
	 * @return The theta parameter.
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * Returns the center coordinate for the specified spatial dimension.
	 *
	 * @param dimension The spatial dimension index.
	 * @return The center value for the given dimension.
	 */
	public double getCenter(final int dimension) {
		return center[dimension];
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public int getNumberOfSpaceGrids() {
		return spaceGrids.size();
	}
}
