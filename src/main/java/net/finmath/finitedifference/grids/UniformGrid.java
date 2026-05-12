package net.finmath.finitedifference.grids;

/**
 * Implementation of the {@link Grid} interface representing a uniform spatial
 * grid.
 *
 * <p>
 * This grid divides the interval between {@code minimumValue} and {@code
 * maximumValue}
 * into equal-sized intervals defined by {@code numberOfSteps}.
 * </p>
 *
 * @author Michela Birtele
 */
public class UniformGrid extends AbstractGrid {

	/**
	 * The number of steps.
	 */
	private int numberOfSteps;
	/**
	 * The minimum value.
	 */
	private double minimumValue;
	/**
	 * The maximum value.
	 */
	private double maximumValue;

	/**
	 * The grid.
	 */
	private double[] grid;
	/**
	 * The delta.
	 */
	private double[] delta;

	/**
	 * Constructs a uniform grid with the specified number of steps
	 * and boundary values.
	 *
	 * @param numberOfSteps The number of intervals in the grid.
	 * @param minimumValue  The lower bound of the grid interval.
	 * @param maximumValue  The upper bound of the grid interval.
	 */
	public UniformGrid(
			final int numberOfSteps,
			final double minimumValue,
			final double maximumValue) {

		this.numberOfSteps = numberOfSteps;
		this.minimumValue = minimumValue;
		this.maximumValue = maximumValue;
	}

	/**
	 * Generates the grid points uniformly spaced between
	 * {@code minimumValue} and {@code maximumValue}.
	 */
	private void generateGrid() {

		final double step = (maximumValue - minimumValue) / numberOfSteps;

		grid = new double[numberOfSteps + 1];

		for (int i = 0; i <= numberOfSteps; i++) {
			grid[i] = minimumValue + i * step;
		}
	}

	/**
	 * Generates the spacing (delta) array as differences between
	 * consecutive grid points.
	 *
	 * @param vector Array of values at grid points (usually the grid itself).
	 */
	private void generateDelta(final double[] vector) {

		if (grid == null) {
			generateGrid();
		}

		delta = new double[numberOfSteps];

		for (int i = 0; i < vector.length - 1; i++) {
			delta[i] = vector[i + 1] - vector[i];
		}
	}

	/**
	 * Returns the grid points, generating them if necessary.
	 *
	 * @return The uniformly spaced grid points.
	 */
	@Override
	public double[] getGrid() {

		if (grid == null) {
			generateGrid();
		}

		return grid;
	}

	/**
	 * Returns the spacing between grid points, generating it if necessary.
	 *
	 * @param vector Array of values at grid points (usually the grid itself).
	 * @return The spacing between consecutive grid points.
	 */
	@Override
	public double[] getDelta(final double[] vector) {

		if (delta == null) {
			generateDelta(vector);
		}

		return delta;
	}
}
