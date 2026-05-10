package net.finmath.finitedifference.assetderivativevaluation.products.internal;

import net.finmath.finitedifference.assetderivativevaluation.models.FiniteDifferenceEquityModel;
import net.finmath.finitedifference.grids.SpaceTimeDiscretization;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;

/**
 * Utility methods for discretely monitored knock-in products.
 *
 * <p>
 * Discretely monitored knock-in products are commonly priced by solving a
 * pre-hit continuation problem and replacing values by an activated
 * continuation value on monitoring dates where the barrier has been breached.
 * This class provides shared helpers for extracting the activated continuation
 * vector from a previously computed activated value surface.
 * </p>
 *
 * <p>
 * The activated surface is assumed to be indexed by space point in the first
 * array dimension and by finite-difference time index in the second array
 * dimension. The time index is inferred from {@code maturity - time}, matching
 * the backward-time convention used by the finite-difference solvers.
 * </p>
 *
 * <p>
 * One-dimensional activated vectors may be interpolated from the activated
 * grid to the target grid if the grids differ. Two-dimensional activated
 * vectors currently require the activated and target grids to match in both
 * state variables.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class DiscreteKnockInActivationSupport {

	private DiscreteKnockInActivationSupport() {
	}

	/**
	 * Tests whether two one-dimensional grids are equal within a given absolute
	 * tolerance.
	 *
	 * @param first The first grid.
	 * @param second The second grid.
	 * @param tolerance The absolute tolerance used pointwise.
	 * @return {@code true} if the grids have the same length and all
	 *     corresponding
	 * grid points differ by at most {@code tolerance}; {@code false} otherwise.
	 */
	public static boolean hasSameGrid(
			final double[] first,
			final double[] second,
			final double tolerance) {

		if (first.length != second.length) {
			return false;
		}

		for (int i = 0; i < first.length; i++) {
			if (Math.abs(first[i] - second[i]) > tolerance) {
				return false;
			}
		}

		return true;
	}

	/**
	 * Builds a zero terminal value vector matching the spatial size of a
	 * discretization.
	 *
	 * <p>
	 * For one-dimensional discretizations, the returned vector has the length
	 * of
	 * the first spatial grid. For two-dimensional discretizations, the returned
	 * vector has the product length of the first and second spatial grids,
	 * using
	 * the same flattened indexing convention as {@link #flatten(int, int,
	 * int)}.
	 * </p>
	 *
	 * @param discretization The space-time discretization.
	 * @return A zero-filled terminal value vector.
	 * @throws IllegalArgumentException Thrown if the discretization is neither
	 *         one-dimensional nor two-dimensional.
	 */
	public static double[] buildZeroTerminalValues(final SpaceTimeDiscretization discretization) {
		final int dims = discretization.getNumberOfSpaceGrids();

		if (dims == 1) {
			final double[] grid = discretization.getSpaceGrid(0).getGrid();
			return new double[grid.length];
		}

		if (dims == 2) {
			final double[] grid0 = discretization.getSpaceGrid(0).getGrid();
			final double[] grid1 = discretization.getSpaceGrid(1).getGrid();
			return new double[grid0.length * grid1.length];
		}

		throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
	}

	/**
	 * Extracts an activated continuation vector at a given event time.
	 *
	 * <p>
	 * The method converts the event time into a solver time index using
	 * {@code maturity - time}. If the resulting time is not an exact time-grid
	 * point, the nearest previous time index is used. The extracted vector is
	 * represented on the spatial grid of {@code targetModel}.
	 * </p>
	 *
	 * @param time The event time.
	 * @param maturity The product maturity.
	 * @param activatedSurface The activated continuation surface, indexed by
	 *        flattened space point and time index.
	 * @param activatedDiscretization The discretization on which the activated
	 *        surface was computed.
	 * @param targetModel The target model whose spatial discretization
	 *     determines
	 *        the returned vector.
	 * @param gridTolerance The absolute tolerance used when comparing grids.
	 * @return The activated continuation vector represented on the target model
	 *     grid.
	 * @throws IllegalArgumentException Thrown if the target discretization is
	 *     neither
	 * one-dimensional nor two-dimensional, or if a two-dimensional activated
	 *         surface is requested on a non-matching target grid.
	 */
	public static double[] getActivatedVectorAt(
			final double time,
			final double maturity,
			final double[][] activatedSurface,
			final SpaceTimeDiscretization activatedDiscretization,
			final FiniteDifferenceEquityModel targetModel,
			final double gridTolerance) {

		final double tau = maturity - time;
		int timeIndex = activatedDiscretization.getTimeDiscretization().getTimeIndex(tau);

		if (timeIndex < 0) {
			timeIndex = activatedDiscretization.getTimeDiscretization()
					.getTimeIndexNearestLessOrEqual(tau);
		}

		final SpaceTimeDiscretization targetDiscretization = targetModel.getSpaceTimeDiscretization();
		final int dims = targetDiscretization.getNumberOfSpaceGrids();

		if (dims == 1) {
			return getActivatedVectorAt1D(
					timeIndex,
					activatedSurface,
					activatedDiscretization,
					targetDiscretization,
					gridTolerance
			);
		}

		if (dims == 2) {
			return getActivatedVectorAt2D(
					timeIndex,
					activatedSurface,
					activatedDiscretization,
					targetDiscretization,
					gridTolerance
			);
		}

		throw new IllegalArgumentException("Only 1D and 2D grids are supported.");
	}

	/**
	 * Extracts a one-dimensional activated vector at a given time index.
	 *
	 * <p>
	 * If source and target grids match within the supplied tolerance, values
	 * are
	 * copied directly. Otherwise, the activated surface column is linearly
	 * interpolated from the source grid to the target grid with constant
	 * extrapolation.
	 * </p>
	 *
	 * @param timeIndex The activated surface time index.
	 * @param activatedSurface The activated continuation surface.
	 * @param sourceDiscretization The discretization of the activated surface.
	 * @param targetDiscretization The target discretization.
	 * @param gridTolerance The absolute tolerance used when comparing grids.
	 * @return The activated vector represented on the target grid.
	 */
	private static double[] getActivatedVectorAt1D(
			final int timeIndex,
			final double[][] activatedSurface,
			final SpaceTimeDiscretization sourceDiscretization,
			final SpaceTimeDiscretization targetDiscretization,
			final double gridTolerance) {

		final double[] targetGrid = targetDiscretization.getSpaceGrid(0).getGrid();
		final double[] sourceGrid = sourceDiscretization.getSpaceGrid(0).getGrid();

		final double[] activatedVector = new double[targetGrid.length];

		if (hasSameGrid(sourceGrid, targetGrid, gridTolerance)) {
			for (int i = 0; i < targetGrid.length; i++) {
				activatedVector[i] = activatedSurface[i][timeIndex];
			}
			return activatedVector;
		}

		final RationalFunctionInterpolation interpolator = new RationalFunctionInterpolation(
				sourceGrid,
				getColumn(activatedSurface, timeIndex),
				InterpolationMethod.LINEAR,
				ExtrapolationMethod.CONSTANT
		);

		for (int i = 0; i < targetGrid.length; i++) {
			activatedVector[i] = interpolator.getValue(targetGrid[i]);
		}

		return activatedVector;
	}

	/**
	 * Extracts a two-dimensional activated vector at a given time index.
	 *
	 * <p>
	 * The method assumes flattened two-dimensional indexing, where
	 * {@code flatten(i, j, n0)} maps the first-state index {@code i} and
	 * second-state index {@code j} into the one-dimensional value vector.
	 * Two-dimensional interpolation is not performed; source and target grids
	 * must match in both state variables within the supplied tolerance.
	 * </p>
	 *
	 * @param timeIndex The activated surface time index.
	 * @param activatedSurface The activated continuation surface.
	 * @param sourceDiscretization The discretization of the activated surface.
	 * @param targetDiscretization The target discretization.
	 * @param gridTolerance The absolute tolerance used when comparing grids.
	 * @return The activated vector represented on the target grid.
	 * @throws IllegalArgumentException Thrown if the source and target grids do
	 *         not match in both state variables.
	 */
	private static double[] getActivatedVectorAt2D(
			final int timeIndex,
			final double[][] activatedSurface,
			final SpaceTimeDiscretization sourceDiscretization,
			final SpaceTimeDiscretization targetDiscretization,
			final double gridTolerance) {

		final double[] targetX0 = targetDiscretization.getSpaceGrid(0).getGrid();
		final double[] targetX1 = targetDiscretization.getSpaceGrid(1).getGrid();
		final double[] sourceX0 = sourceDiscretization.getSpaceGrid(0).getGrid();
		final double[] sourceX1 = sourceDiscretization.getSpaceGrid(1).getGrid();

		if (!hasSameGrid(sourceX0, targetX0, gridTolerance)
				|| !hasSameGrid(sourceX1, targetX1, gridTolerance)) {
			throw new IllegalArgumentException(
					"Discrete 2D knock-in currently requires activated and target grids to match."
			);
		}

		final int n0 = targetX0.length;
		final int n1 = targetX1.length;
		final double[] activatedVector = new double[n0 * n1];

		for (int j = 0; j < n1; j++) {
			for (int i = 0; i < n0; i++) {
				final int k = flatten(i, j, n0);
				activatedVector[k] = activatedSurface[k][timeIndex];
			}
		}

		return activatedVector;
	}

	/**
	 * Flattens a two-dimensional grid index into a one-dimensional vector
	 * index.
	 *
	 * @param i0 The index in the first spatial dimension.
	 * @param i1 The index in the second spatial dimension.
	 * @param n0 The number of grid points in the first spatial dimension.
	 * @return The flattened vector index.
	 */
	public static int flatten(final int i0, final int i1, final int n0) {
		return i0 + i1 * n0;
	}

	/**
	 * Extracts a column from a rectangular matrix.
	 *
	 * @param matrix The matrix.
	 * @param columnIndex The column index to extract.
	 * @return The requested matrix column.
	 */
	public static double[] getColumn(final double[][] matrix, final int columnIndex) {
		final double[] column = new double[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			column[i] = matrix[i][columnIndex];
		}
		return column;
	}
}
