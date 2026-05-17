package net.finmath.finitedifference.grids;

import net.finmath.functions.NormalDistribution;

/**
 * Non-uniform grid based on the Gaussian (normal) distribution between
 * {@code minimumValue} and {@code maximumValue}.
 *
 * <p>
 * The grid points are distributed according to the cumulative distribution
 * function (CDF) of a normal distribution with mean {@code centering} and
 * standard deviation {@code sigma}. This results in a higher density of
 * points near the {@code centering} value and sparser points further away.
 * </p>
 *
 * <p>
 * The grid is constructed by mapping uniformly spaced probabilities between
 * the CDF values at {@code minimumValue} and {@code maximumValue} back to
 * the original domain using the inverse CDF (quantile function).
 * </p>
 *
 * @author Michela Birtele
 */
public class GaussianGrid extends AbstractGrid {

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
	 * The centering.
	 */
	private double centering;
	/**
	 * The sigma.
	 */
	private double sigma;

	/**
	 * The grid.
	 */
	private double[] grid;
	/**
	 * The delta.
	 */
	private double[] delta;

	/**
	 * Constructs a Gaussian-distributed grid.
	 *
	 * @param numberOfSteps The number of intervals in the grid.
	 * @param minimumValue  The lower bound of the grid.
	 * @param maximumValue  The upper bound of the grid.
	 * @param centering     The mean (center) of the Gaussian distribution.
	 * @param sigma         The standard deviation controlling the spread
	 *                      around the centering.
	 */
	public GaussianGrid(
			final int numberOfSteps,
			final double minimumValue,
			final double maximumValue,
			final double centering,
			final double sigma) {

		super();
		this.numberOfSteps = numberOfSteps;
		this.minimumValue = minimumValue;
		this.maximumValue = maximumValue;
		this.centering = centering;
		this.sigma = sigma;
	}

	/**
	 * Generates the grid points distributed according to the Gaussian distribution.
	 */
	private void generateGrid() {

		grid = new double[numberOfSteps + 1];

		final double pMin =
				NormalDistribution.cumulativeDistribution(
						(minimumValue - centering) / sigma);
		final double pMax =
				NormalDistribution.cumulativeDistribution(
						(maximumValue - centering) / sigma);

		for (int i = 0; i <= numberOfSteps; i++) {
			final double p =
					pMin + (i / (double) numberOfSteps) * (pMax - pMin);
			grid[i] =
					centering + sigma
						 * NormalDistribution.inverseCumulativeDistribution(p);
		}

		grid[0] = minimumValue;
		grid[numberOfSteps] = maximumValue;
	}

	/**
	 * Computes the differences (delta) between consecutive grid points.
	 *
	 * @param vector Array of values at grid points (typically the grid itself).
	 */
	private void generateDelta(final double[] vector) {

		delta = new double[numberOfSteps];

		for (int i = 0; i < vector.length - 1; i++) {
			delta[i] = vector[i + 1] - vector[i];
		}
	}

	/**
	 * Returns the generated grid points, generating them if necessary.
	 *
	 * @return The grid points distributed via Gaussian quantiles.
	 */
	@Override
	public double[] getGrid() {

		if (grid == null) {
			generateGrid();
		}

		return grid;
	}

	/**
	 * Returns the spacing (delta) between consecutive grid points,
	 * generating it if necessary.
	 *
	 * @param vector Array of values at grid points (usually the grid itself).
	 * @return The spacing between adjacent grid points.
	 */
	@Override
	public double[] getDelta(final double[] vector) {

		if (delta == null) {
			generateDelta(vector);
		}

		return delta;
	}
}
