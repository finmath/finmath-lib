package net.finmath.finitedifference.utilities;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.time.TimeDiscretization;

/**
 * Utility class for constructing Greek surfaces from finite-difference values.
 *
 * <p>
 * The returned Greek surfaces use the same representation as product value
 * surfaces: {@code double[flattenedSpaceIndex][timeIndex]}.
 * </p>
 *
 * <p>
 * Spatial derivatives are computed using local finite-difference formulas on
 * the model grid. Interior first and second derivatives use three-point
 * non-uniform-grid formulas. Boundary first derivatives use one-sided
 * two-point slopes. Boundary second derivatives use the nearest three-point
 * stencil.
 * </p>
 *
 * <p>
 * The finite-difference time grid is interpreted as time-to-maturity. Therefore
 * {@link #thetaSurface(double[][], SpaceTimeDiscretization)} returns the
 * derivative with respect to calendar evaluation time, which is the negative of
 * the derivative with respect to time-to-maturity.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class FiniteDifferenceGreekProvider {

	private FiniteDifferenceGreekProvider() {
	}

	/**
	 * Performs the operation.
	 *
	 * @param greek The value.
	 * @param values The value.
	 * @param discretization The value.
	 * @return The value.
	 */
	public static double[][] greekSurface(
			final FiniteDifferenceGreek greek,
			final double[][] values,
			final SpaceTimeDiscretization discretization) {

		if (greek == null) {
			throw new IllegalArgumentException("greek must not be null.");
		}

		switch (greek) {
		case DELTA:
			return deltaSurface(values, discretization);
		case GAMMA:
			return gammaSurface(values, discretization);
		case THETA:
			return thetaSurface(values, discretization);
		default:
			throw new IllegalArgumentException("Unsupported Greek: " + greek);
		}
	}

	/**
	 * Performs the operation.
	 *
	 * @param values The value.
	 * @param discretization The value.
	 * @return The value.
	 */
	public static double[][] deltaSurface(
			final double[][] values,
			final SpaceTimeDiscretization discretization) {
		return firstDerivativeSurface(values, discretization, 0);
	}

	/**
	 * Performs the operation.
	 *
	 * @param values The value.
	 * @param discretization The value.
	 * @return The value.
	 */
	public static double[][] gammaSurface(
			final double[][] values,
			final SpaceTimeDiscretization discretization) {
		return secondDerivativeSurface(values, discretization, 0);
	}

	/**
	 * Performs the operation.
	 *
	 * @param values The value.
	 * @param discretization The value.
	 * @param dimension The value.
	 * @return The value.
	 */
	public static double[][] firstDerivativeSurface(
			final double[][] values,
			final SpaceTimeDiscretization discretization,
			final int dimension) {

		final FiniteDifferenceGridLayout layout = FiniteDifferenceGridLayout.of(discretization);
		layout.validateSurface(values);
		validateDimension(layout, dimension);

		final double[] grid = discretization.getSpaceGrid(dimension).getGrid();

		if (grid.length < 2) {
			throw new IllegalArgumentException("First derivatives require at least two grid points.");
		}

		final double[][] derivative = createEmptySurfaceLike(values);

		for (int flatIndex = 0; flatIndex < values.length; flatIndex++) {
			final int[] indices = layout.unflatten(flatIndex);
			final int index = indices[dimension];

			for (int timeIndex = 0; timeIndex < values[flatIndex].length; timeIndex++) {
				derivative[flatIndex][timeIndex] = firstDerivativeAt(
						values,
						layout,
						indices,
						dimension,
						index,
						grid,
						timeIndex
						);
			}
		}

		return derivative;
	}

	/**
	 * Performs the operation.
	 *
	 * @param values The value.
	 * @param discretization The value.
	 * @param dimension The value.
	 * @return The value.
	 */
	public static double[][] secondDerivativeSurface(
			final double[][] values,
			final SpaceTimeDiscretization discretization,
			final int dimension) {

		final FiniteDifferenceGridLayout layout = FiniteDifferenceGridLayout.of(discretization);
		layout.validateSurface(values);
		validateDimension(layout, dimension);

		final double[] grid = discretization.getSpaceGrid(dimension).getGrid();

		if (grid.length < 3) {
			throw new IllegalArgumentException("Second derivatives require at least three grid points.");
		}

		final double[][] derivative = createEmptySurfaceLike(values);

		for (int flatIndex = 0; flatIndex < values.length; flatIndex++) {
			final int[] indices = layout.unflatten(flatIndex);
			final int index = indices[dimension];

			for (int timeIndex = 0; timeIndex < values[flatIndex].length; timeIndex++) {
				derivative[flatIndex][timeIndex] = secondDerivativeAt(
						values,
						layout,
						indices,
						dimension,
						index,
						grid,
						timeIndex
						);
			}
		}

		return derivative;
	}

	/**
	 * Performs the operation.
	 *
	 * @param values The value.
	 * @param discretization The value.
	 * @param firstDimension The value.
	 * @param secondDimension The value.
	 * @return The value.
	 */
	public static double[][] mixedSecondDerivativeSurface(
			final double[][] values,
			final SpaceTimeDiscretization discretization,
			final int firstDimension,
			final int secondDimension) {

		if (firstDimension == secondDimension) {
			return secondDerivativeSurface(values, discretization, firstDimension);
		}

		final double[][] firstDerivative =
				firstDerivativeSurface(values, discretization, firstDimension);

		return firstDerivativeSurface(firstDerivative, discretization, secondDimension);
	}

	/**
	 * Performs the operation.
	 *
	 * @param values The value.
	 * @param discretization The value.
	 * @return The value.
	 */
	public static double[][] thetaSurface(
			final double[][] values,
			final SpaceTimeDiscretization discretization) {

		final FiniteDifferenceGridLayout layout = FiniteDifferenceGridLayout.of(discretization);
		layout.validateSurface(values);

		if (values.length == 0 || values[0].length < 2) {
			throw new IllegalArgumentException("Theta requires at least two time points.");
		}

		final TimeDiscretization timeDiscretization = discretization.getTimeDiscretization();

		if (timeDiscretization.getNumberOfTimes() != values[0].length) {
			throw new IllegalArgumentException(
					"Value surface time dimension does not match the time discretization."
					);
		}

		final double[][] theta = createEmptySurfaceLike(values);

		for (int flatIndex = 0; flatIndex < values.length; flatIndex++) {
			for (int timeIndex = 0; timeIndex < values[flatIndex].length; timeIndex++) {
				theta[flatIndex][timeIndex] = -timeToMaturityDerivativeAt(
						values[flatIndex],
						timeDiscretization,
						timeIndex
						);
			}
		}

		return theta;
	}

	private static double firstDerivativeAt(
			final double[][] values,
			final FiniteDifferenceGridLayout layout,
			final int[] indices,
			final int dimension,
			final int index,
			final double[] grid,
			final int timeIndex) {

		if (index == 0) {
			return forwardSlope(values, layout, indices, dimension, 0, 1, grid, timeIndex);
		}

		if (index == grid.length - 1) {
			return forwardSlope(
					values,
					layout,
					indices,
					dimension,
					grid.length - 2,
					grid.length - 1,
					grid,
					timeIndex
					);
		}

		return threePointFirstDerivative(
				valueAt(values, layout, indices, dimension, index - 1, timeIndex),
				valueAt(values, layout, indices, dimension, index, timeIndex),
				valueAt(values, layout, indices, dimension, index + 1, timeIndex),
				grid[index - 1],
				grid[index],
				grid[index + 1]
				);
	}

	private static double secondDerivativeAt(
			final double[][] values,
			final FiniteDifferenceGridLayout layout,
			final int[] indices,
			final int dimension,
			final int index,
			final double[] grid,
			final int timeIndex) {

		final int lowerIndex = Math.max(0, Math.min(index - 1, grid.length - 3));
		final int centerIndex = lowerIndex + 1;
		final int upperIndex = lowerIndex + 2;

		return threePointSecondDerivative(
				valueAt(values, layout, indices, dimension, lowerIndex, timeIndex),
				valueAt(values, layout, indices, dimension, centerIndex, timeIndex),
				valueAt(values, layout, indices, dimension, upperIndex, timeIndex),
				grid[lowerIndex],
				grid[centerIndex],
				grid[upperIndex]
				);
	}

	private static double timeToMaturityDerivativeAt(
			final double[] row,
			final TimeDiscretization timeDiscretization,
			final int timeIndex) {

		if (timeIndex == 0) {
			return (row[1] - row[0])
					/ (timeDiscretization.getTime(1) - timeDiscretization.getTime(0));
		}

		if (timeIndex == row.length - 1) {
			return (row[row.length - 1] - row[row.length - 2])
					/ (timeDiscretization.getTime(row.length - 1)
							- timeDiscretization.getTime(row.length - 2));
		}

		return threePointFirstDerivative(
				row[timeIndex - 1],
				row[timeIndex],
				row[timeIndex + 1],
				timeDiscretization.getTime(timeIndex - 1),
				timeDiscretization.getTime(timeIndex),
				timeDiscretization.getTime(timeIndex + 1)
				);
	}

	private static double forwardSlope(
			final double[][] values,
			final FiniteDifferenceGridLayout layout,
			final int[] indices,
			final int dimension,
			final int lowerIndex,
			final int upperIndex,
			final double[] grid,
			final int timeIndex) {

		return (valueAt(values, layout, indices, dimension, upperIndex, timeIndex)
				- valueAt(values, layout, indices, dimension, lowerIndex, timeIndex))
				/ (grid[upperIndex] - grid[lowerIndex]);
	}

	private static double threePointFirstDerivative(
			final double f0,
			final double f1,
			final double f2,
			final double x0,
			final double x1,
			final double x2) {

		return f0 * (x1 - x2) / ((x0 - x1) * (x0 - x2))
				+ f1 * (2.0 * x1 - x0 - x2) / ((x1 - x0) * (x1 - x2))
				+ f2 * (x1 - x0) / ((x2 - x0) * (x2 - x1));
	}

	private static double threePointSecondDerivative(
			final double f0,
			final double f1,
			final double f2,
			final double x0,
			final double x1,
			final double x2) {

		return 2.0 * (
				f0 / ((x0 - x1) * (x0 - x2))
				+ f1 / ((x1 - x0) * (x1 - x2))
				+ f2 / ((x2 - x0) * (x2 - x1))
				);
	}

	private static double valueAt(
			final double[][] values,
			final FiniteDifferenceGridLayout layout,
			final int[] indices,
			final int dimension,
			final int dimensionIndex,
			final int timeIndex) {

		final int[] shiftedIndices = indices.clone();
		shiftedIndices[dimension] = dimensionIndex;

		return values[layout.flatten(shiftedIndices)][timeIndex];
	}

	private static double[][] createEmptySurfaceLike(final double[][] values) {
		final double[][] result = new double[values.length][];

		for (int i = 0; i < values.length; i++) {
			result[i] = new double[values[i].length];
		}

		return result;
	}

	private static void validateDimension(
			final FiniteDifferenceGridLayout layout,
			final int dimension) {

		if (dimension < 0 || dimension >= layout.getDimension()) {
			throw new IllegalArgumentException("dimension out of range.");
		}
	}
}
