package net.finmath.finitedifference.utilities;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.time.TimeDiscretization;

/**
 * Interpolation utilities for finite-difference value vectors and surfaces.
 *
 * <p>
 * The methods in this class operate on the standard finite-difference output
 * format used by products:
 * </p>
 *
 * <ul>
 * <li>{@code double[]} for one time slice, indexed by flattened spatial
 * index,</li>
 * <li>{@code double[][]} for full value surfaces, indexed by flattened spatial
 *       index and solver time index.</li>
 * </ul>
 *
 * <p>
 * Spatial interpolation is multilinear and supports any number of spatial
 * dimensions. Coordinates outside the grid domain are extrapolated constantly
 * by clamping to the nearest boundary grid point.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class FiniteDifferenceValueInterpolator {

	private FiniteDifferenceValueInterpolator() {
	}

	/**
	 * Interpolates a value vector at a spatial coordinate.
	 *
	 * @param values The value vector indexed by flattened spatial index.
	 * @param discretization The space-time discretization defining the spatial
	 *     grids.
	 * @param coordinates The spatial coordinates.
	 * @return The interpolated value.
	 */
	public static double interpolateValue(
			final double[] values,
			final SpaceTimeDiscretization discretization,
			final double... coordinates) {

		final FiniteDifferenceGridLayout layout = FiniteDifferenceGridLayout.of(discretization);
		layout.validateVector(values);

		if (coordinates == null || coordinates.length != layout.getDimension()) {
			throw new IllegalArgumentException("The number of coordinates must match the spatial dimension.");
		}

		final Bracket[] brackets = buildBrackets(discretization, coordinates);

		double value = 0.0;
		final int numberOfCorners = 1 << layout.getDimension();

		for (int corner = 0; corner < numberOfCorners; corner++) {
			final int[] indices = new int[layout.getDimension()];
			double weight = 1.0;

			for (int dimension = 0; dimension < layout.getDimension(); dimension++) {
				final boolean upperCorner = ((corner >> dimension) & 1) == 1;
				final Bracket bracket = brackets[dimension];

				if (bracket.lowerIndex == bracket.upperIndex) {
					if (upperCorner) {
						weight = 0.0;
						break;
					}

					indices[dimension] = bracket.lowerIndex;
				} else if (upperCorner) {
					indices[dimension] = bracket.upperIndex;
					weight *= bracket.upperWeight;
				} else {
					indices[dimension] = bracket.lowerIndex;
					weight *= 1.0 - bracket.upperWeight;
				}
			}

			if (weight != 0.0) {
				value += weight * values[layout.flatten(indices)];
			}
		}

		return value;
	}

	/**
	 * Interpolates a value surface at an evaluation time and spatial
	 * coordinate.
	 *
	 * <p>
	 * The method converts the calendar evaluation time to solver backward time
	 * by using {@code tau = maturity - evaluationTime}. The nearest previous
	 * time index is used.
	 * </p>
	 *
	 * @param values The value surface indexed by flattened spatial index and
	 *     time index.
	 * @param discretization The space-time discretization.
	 * @param evaluationTime The evaluation time.
	 * @param maturity The product maturity.
	 * @param coordinates The spatial coordinates.
	 * @return The interpolated value.
	 */
	public static double interpolateSurface(
			final double[][] values,
			final SpaceTimeDiscretization discretization,
			final double evaluationTime,
			final double maturity,
			final double... coordinates) {

		final int timeIndex = getTimeIndexNearestLessOrEqual(
				discretization,
				evaluationTime,
				maturity,
				values
		);

		return interpolateTimeIndex(values, discretization, timeIndex, coordinates);
	}

	/**
	 * Interpolates a value surface at a given solver time index and spatial
	 * coordinate.
	 *
	 * @param values The value surface indexed by flattened spatial index and
	 *     time index.
	 * @param discretization The space-time discretization.
	 * @param timeIndex The solver time index.
	 * @param coordinates The spatial coordinates.
	 * @return The interpolated value.
	 */
	public static double interpolateTimeIndex(
			final double[][] values,
			final SpaceTimeDiscretization discretization,
			final int timeIndex,
			final double... coordinates) {

		final double[] timeSlice = getTimeSlice(values, discretization, timeIndex);
		return interpolateValue(timeSlice, discretization, coordinates);
	}

	/**
	 * Extracts a value vector at an evaluation time.
	 *
	 * @param values The value surface indexed by flattened spatial index and
	 *     time index.
	 * @param discretization The space-time discretization.
	 * @param evaluationTime The evaluation time.
	 * @param maturity The product maturity.
	 * @return The value vector at the nearest previous solver time index.
	 */
	public static double[] getTimeSlice(
			final double[][] values,
			final SpaceTimeDiscretization discretization,
			final double evaluationTime,
			final double maturity) {

		final int timeIndex = getTimeIndexNearestLessOrEqual(
				discretization,
				evaluationTime,
				maturity,
				values
		);

		return getTimeSlice(values, discretization, timeIndex);
	}

	/**
	 * Extracts a value vector at a solver time index.
	 *
	 * @param values The value surface indexed by flattened spatial index and
	 *     time index.
	 * @param discretization The space-time discretization.
	 * @param timeIndex The solver time index.
	 * @return The value vector at the requested time index.
	 */
	public static double[] getTimeSlice(
			final double[][] values,
			final SpaceTimeDiscretization discretization,
			final int timeIndex) {

		final FiniteDifferenceGridLayout layout = FiniteDifferenceGridLayout.of(discretization);
		layout.validateSurface(values);

		if (values.length == 0 || values[0].length == 0) {
			throw new IllegalArgumentException("values must contain at least one time index.");
		}
		if (timeIndex < 0 || timeIndex >= values[0].length) {
			throw new IllegalArgumentException("timeIndex out of range.");
		}

		final double[] timeSlice = new double[values.length];
		for (int i = 0; i < values.length; i++) {
			timeSlice[i] = values[i][timeIndex];
		}

		return timeSlice;
	}

	/**
	 * Returns the nearest previous solver time index for an evaluation time.
	 *
	 * @param discretization The space-time discretization.
	 * @param evaluationTime The evaluation time.
	 * @param maturity The product maturity.
	 * @param values The value surface used for time-index bounds.
	 * @return The nearest previous solver time index.
	 */
	public static int getTimeIndexNearestLessOrEqual(
			final SpaceTimeDiscretization discretization,
			final double evaluationTime,
			final double maturity,
			final double[][] values) {

		final FiniteDifferenceGridLayout layout = FiniteDifferenceGridLayout.of(discretization);
		layout.validateSurface(values);

		if (values.length == 0 || values[0].length == 0) {
			throw new IllegalArgumentException("values must contain at least one time index.");
		}

		final TimeDiscretization timeDiscretization = discretization.getTimeDiscretization();
		final double tau = maturity - evaluationTime;

		int timeIndex = timeDiscretization.getTimeIndex(tau);
		if (timeIndex < 0) {
			timeIndex = timeDiscretization.getTimeIndexNearestLessOrEqual(tau);
		}

		if (timeIndex < 0) {
			timeIndex = 0;
		}
		if (timeIndex >= values[0].length) {
			timeIndex = values[0].length - 1;
		}

		return timeIndex;
	}

	private static Bracket[] buildBrackets(
			final SpaceTimeDiscretization discretization,
			final double[] coordinates) {

		final Bracket[] brackets = new Bracket[coordinates.length];

		for (int dimension = 0; dimension < coordinates.length; dimension++) {
			brackets[dimension] = getBracket(
					discretization.getSpaceGrid(dimension).getGrid(),
					coordinates[dimension]
			);
		}

		return brackets;
	}

	private static Bracket getBracket(final double[] grid, final double coordinate) {
		if (grid.length == 1) {
			return new Bracket(0, 0, 0.0);
		}

		if (coordinate <= grid[0]) {
			return new Bracket(0, 0, 0.0);
		}

		if (coordinate >= grid[grid.length - 1]) {
			return new Bracket(grid.length - 1, grid.length - 1, 0.0);
		}

		int upperIndex = 1;
		while (upperIndex < grid.length && grid[upperIndex] < coordinate) {
			upperIndex++;
		}

		final int lowerIndex = upperIndex - 1;
		final double upperWeight =
				(coordinate - grid[lowerIndex]) / (grid[upperIndex] - grid[lowerIndex]);

		return new Bracket(lowerIndex, upperIndex, upperWeight);
	}

	private static final class Bracket {

		/**
		 * The lower index.
		 */
		private final int lowerIndex;
		/**
		 * The upper index.
		 */
		private final int upperIndex;
		/**
		 * The upper weight.
		 */
		private final double upperWeight;

		private Bracket(
				final int lowerIndex,
				final int upperIndex,
				final double upperWeight) {
			this.lowerIndex = lowerIndex;
			this.upperIndex = upperIndex;
			this.upperWeight = upperWeight;
		}
	}
}
