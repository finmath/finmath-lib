package net.finmath.finitedifference.grids;

import java.util.Arrays;

/**
 * Piecewise-uniform grid on {@code [minimumValue, maximumValue]} with one
 * mandatory interior node.
 *
 * <p>
 * The interval is split into two sub-intervals:
 * </p>
 * <ul>
 *   <li>{@code [minimumValue, mandatoryPoint]}</li>
 *   <li>{@code [mandatoryPoint, maximumValue]}</li>
 * </ul>
 *
 * <p>
 * On each side, nodes are distributed uniformly. The mandatory point is forced
 * to be an exact grid node, while the lower and upper bounds are preserved.
 * </p>
 *
 * <p>
 * This is useful for finite-difference pricing of products with a payoff
 * discontinuity or kink at a known location, for example digitals with
 * discontinuity at the strike.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public class GridWithMandatoryPoint extends AbstractGrid {

	/**
	 * The number of steps.
	 */
	private final int numberOfSteps;
	/**
	 * The minimum value.
	 */
	private final double minimumValue;
	/**
	 * The maximum value.
	 */
	private final double maximumValue;
	/**
	 * The mandatory point.
	 */
	private final double mandatoryPoint;

	/**
	 * The grid.
	 */
	private double[] grid;
	/**
	 * The delta.
	 */
	private double[] delta;

	/**
	 * Creates a grid with one mandatory interior node.
	 *
	 * @param numberOfSteps Number of grid intervals.
	 * @param minimumValue Lower grid boundary.
	 * @param maximumValue Upper grid boundary.
	 * @param mandatoryPoint Point that must appear exactly as a grid node.
	 */
	public GridWithMandatoryPoint(
			final int numberOfSteps,
			final double minimumValue,
			final double maximumValue,
			final double mandatoryPoint) {

		validateInputs(numberOfSteps, minimumValue, maximumValue, mandatoryPoint);

		this.numberOfSteps = numberOfSteps;
		this.minimumValue = minimumValue;
		this.maximumValue = maximumValue;
		this.mandatoryPoint = mandatoryPoint;
	}

	/**
	 * Generates the grid.
	 *
	 * <p>
	 * The mandatory point is assigned an interior node index chosen according
	 * to
	 * its relative position inside the interval. The left and right parts are
	 * then filled uniformly.
	 * </p>
	 */
	private void generateGrid() {

		final int mandatoryIndex = chooseMandatoryIndex(
				numberOfSteps,
				minimumValue,
				maximumValue,
				mandatoryPoint
				);

		grid = new double[numberOfSteps + 1];

		final double dxLeft = (mandatoryPoint - minimumValue) / mandatoryIndex;
		final double dxRight = (maximumValue - mandatoryPoint) / (numberOfSteps - mandatoryIndex);

		for (int i = 0; i <= mandatoryIndex; i++) {
			grid[i] = minimumValue + i * dxLeft;
		}

		for (int i = mandatoryIndex + 1; i <= numberOfSteps; i++) {
			grid[i] = mandatoryPoint + (i - mandatoryIndex) * dxRight;
		}

		grid[0] = minimumValue;
		grid[mandatoryIndex] = mandatoryPoint;
		grid[numberOfSteps] = maximumValue;
	}

	/**
	 * Generates the spacing array.
	 *
	 * @param vector Array of values at grid points, usually the grid itself.
	 */
	private void generateDelta(final double[] vector) {

		if (grid == null) {
			generateGrid();
		}

		if (vector == null) {
			throw new IllegalArgumentException("vector must not be null.");
		}

		if (vector.length != grid.length) {
			throw new IllegalArgumentException("vector length must match grid length.");
		}

		delta = new double[numberOfSteps];

		for (int i = 0; i < vector.length - 1; i++) {
			delta[i] = vector[i + 1] - vector[i];
		}
	}

	/**
	 * Returns the grid points, generating them if necessary.
	 *
	 * @return The grid points.
	 */
	@Override
	public double[] getGrid() {

		if (grid == null) {
			generateGrid();
		}

		return Arrays.copyOf(grid, grid.length);
	}

	/**
	 * Returns the spacing between consecutive grid points.
	 *
	 * @param vector Array of values at grid points, usually the grid itself.
	 * @return The spacing array.
	 */
	@Override
	public double[] getDelta(final double[] vector) {

		if (delta == null) {
			generateDelta(vector);
		}

		return Arrays.copyOf(delta, delta.length);
	}

	/**
	 * Returns the mandatory point.
	 *
	 * @return The mandatory point.
	 */
	public double getMandatoryPoint() {
		return mandatoryPoint;
	}

	/**
	 * Returns the node index corresponding to the mandatory point.
	 *
	 * @return The mandatory node index.
	 */
	public int getMandatoryIndex() {
		return chooseMandatoryIndex(numberOfSteps, minimumValue, maximumValue, mandatoryPoint);
	}

	/**
	 * Finds the index of a mandatory point in a previously generated grid.
	 *
	 * @param grid The grid array.
	 * @param mandatoryPoint The mandatory point.
	 * @return The index of the mandatory point.
	 */
	public static int findMandatoryIndex(final double[] grid, final double mandatoryPoint) {
		for (int i = 0; i < grid.length; i++) {
			if (Math.abs(grid[i] - mandatoryPoint) < 1E-12) {
				return i;
			}
		}
		throw new IllegalArgumentException("Mandatory point is not a grid node.");
	}

	/**
	 * Convenience overload.
	 *
	 * @param grid The grid.
	 * @param mandatoryPoint The mandatory point.
	 * @return The index of the mandatory point.
	 */
	public static int findMandatoryIndex(final Grid grid, final double mandatoryPoint) {
		return findMandatoryIndex(grid.getGrid(), mandatoryPoint);
	}

	/**
	 * Chooses the interior node index assigned to the mandatory point.
	 *
	 * <p>
	 * The allocation is chosen to roughly match the geometric position of the
	 * mandatory point inside the interval.
	 * </p>
	 *
	 * @param numberOfSteps Number of grid intervals.
	 * @param minimumValue Lower grid boundary.
	 * @param maximumValue Upper grid boundary.
	 * @param mandatoryPoint Mandatory point.
	 * @return The grid node index assigned to the mandatory point.
	 */
	private static int chooseMandatoryIndex(
			final int numberOfSteps,
			final double minimumValue,
			final double maximumValue,
			final double mandatoryPoint) {

		final double relativePosition =
				(mandatoryPoint - minimumValue) / (maximumValue - minimumValue);

		int mandatoryIndex = (int)Math.round(relativePosition * numberOfSteps);

		mandatoryIndex = Math.max(1, mandatoryIndex);
		mandatoryIndex = Math.min(numberOfSteps - 1, mandatoryIndex);

		return mandatoryIndex;
	}

	/**
	 * Validates constructor inputs.
	 *
	 * @param numberOfSteps Number of grid intervals.
	 * @param minimumValue Lower grid boundary.
	 * @param maximumValue Upper grid boundary.
	 * @param mandatoryPoint Mandatory point.
	 */
	private static void validateInputs(
			final int numberOfSteps,
			final double minimumValue,
			final double maximumValue,
			final double mandatoryPoint) {

		if (numberOfSteps < 2) {
			throw new IllegalArgumentException("numberOfSteps must be at least 2.");
		}

		if (!(minimumValue < maximumValue)) {
			throw new IllegalArgumentException("Require minimumValue < maximumValue.");
		}

		if (!(mandatoryPoint > minimumValue && mandatoryPoint < maximumValue)) {
			throw new IllegalArgumentException(
					"mandatoryPoint must lie strictly inside (minimumValue, maximumValue)."
					);
		}
	}
}
