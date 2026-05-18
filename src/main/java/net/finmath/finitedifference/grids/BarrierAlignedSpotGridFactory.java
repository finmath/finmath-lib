package net.finmath.finitedifference.grids;

import java.util.Arrays;

/**
 * Utility factory for spot grids used in barrier option PDE problems.
 *
 * <p>
 * Main features:
 * </p>
 * <ul>
 *   <li>forces the barrier to be an exact grid node,</li>
 *   <li>can build either a uniform or a clustered grid,</li>
 *   <li>clustered grid puts more points near the barrier.</li>
 * </ul>
 *
 * <p>
 * The clustered grid is piecewise-defined:
 * on each side of the barrier, nodes are generated with a power transform.
 * For clusteringExponent {@code > 1.0}, points concentrate near the barrier.
 * For clusteringExponent {@code = 1.0}, the grid is piecewise uniform.
 * </p>
 *
 * <p>
 * This class is intended especially for direct barrier pricing under
 * Heston/SABR,
 * where barrier alignment can materially improve knock-in accuracy.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class BarrierAlignedSpotGridFactory {

	private BarrierAlignedSpotGridFactory() {
	}

	/**
	 * Creates a uniform grid with the barrier exactly on a node.
	 *
	 * <p>
	 * The spacing is chosen so that:
	 * </p>
	 * <ul>
	 *   <li>the barrier lies exactly on a node,</li>
	 *   <li>the lower/upper bounds are preserved,</li>
	 *   <li>the number of intervals is exactly {@code numberOfSteps}.</li>
	 * </ul>
	 *
	 * <p>
	 * This method requires the barrier to lie strictly inside the interval.
	 * </p>
	 *
	 * @param numberOfSteps Number of grid intervals.
	 * @param sMin Lower grid boundary.
	 * @param sMax Upper grid boundary.
	 * @param barrier Barrier level.
	 * @return A grid with the barrier exactly on a node.
	 */
	public static Grid createBarrierAlignedUniformGrid(
			final int numberOfSteps,
			final double sMin,
			final double sMax,
			final double barrier) {

		validateInputs(numberOfSteps, sMin, sMax, barrier);

		final int barrierIndex = chooseBarrierIndex(numberOfSteps, sMin, sMax, barrier);

		final double[] grid = new double[numberOfSteps + 1];
		final double dxLeft = (barrier - sMin) / barrierIndex;
		final double dxRight = (sMax - barrier) / (numberOfSteps - barrierIndex);

		/*
		 * If dxLeft and dxRight differ, one cannot have a globally uniform grid
		 * with barrier fixed as an exact node and both endpoints preserved.
		 *
		 * So this "uniform" builder is best understood as:
		 * - uniform on the left of the barrier
		 * - uniform on the right of the barrier
		 * - exact barrier alignment
		 *
		 * This is often already sufficient and far better than missing the
		 * barrier.
		 */
		for (int i = 0; i <= barrierIndex; i++) {
			grid[i] = sMin + i * dxLeft;
		}
		for (int i = barrierIndex + 1; i <= numberOfSteps; i++) {
			grid[i] = barrier + (i - barrierIndex) * dxRight;
		}

		grid[0] = sMin;
		grid[barrierIndex] = barrier;
		grid[numberOfSteps] = sMax;

		return new ArrayGrid(grid);
	}

	/**
	 * Creates a barrier-aligned clustered grid.
	 *
	 * <p>
	 * The barrier is exactly on a node. Points are concentrated near the
	 * barrier
	 * using a power transform on each side:
	 * </p>
	 *
	 * <pre>
	 * x = barrier - (barrier - sMin) * (1 - u)^p    on the left side
	 * x = barrier + (sMax - barrier) * u^p          on the right side
	 * </pre>
	 *
	 * <p>
	 * where {@code p = clusteringExponent}.
	 * </p>
	 *
	 * <ul>
	 *   <li>{@code clusteringExponent = 1.0}: piecewise uniform</li>
	 * <li>{@code clusteringExponent > 1.0}: points cluster near the
	 * barrier</li>
	 * </ul>
	 *
	 * @param numberOfSteps Number of grid intervals.
	 * @param sMin Lower grid boundary.
	 * @param sMax Upper grid boundary.
	 * @param barrier Barrier level.
	 * @param clusteringExponent Clustering strength, must be >= 1.0.
	 * @return A clustered grid with the barrier exactly on a node.
	 */
	public static Grid createBarrierAlignedClusteredGrid(
			final int numberOfSteps,
			final double sMin,
			final double sMax,
			final double barrier,
			final double clusteringExponent) {

		validateInputs(numberOfSteps, sMin, sMax, barrier);

		if (clusteringExponent < 1.0) {
			throw new IllegalArgumentException("clusteringExponent must be >= 1.0");
		}

		final int barrierIndex = chooseBarrierIndex(numberOfSteps, sMin, sMax, barrier);

		final double[] grid = new double[numberOfSteps + 1];

		/*
		 * Left side: i = 0,...,barrierIndex
		 * u runs from 0 to 1, with x(barrierIndex) = barrier.
		 */
		for (int i = 0; i <= barrierIndex; i++) {
			final double u = barrierIndex == 0 ? 0.0 : ((double)i) / barrierIndex;
			grid[i] = barrier - (barrier - sMin) * Math.pow(1.0 - u, clusteringExponent);
		}

		/*
		 * Right side: i = barrierIndex,...,numberOfSteps
		 * u runs from 0 to 1, with x(barrierIndex) = barrier.
		 */
		for (int i = barrierIndex; i <= numberOfSteps; i++) {
			final double u = numberOfSteps == barrierIndex ? 0.0
					: ((double)(i - barrierIndex)) / (numberOfSteps - barrierIndex);
			grid[i] = barrier + (sMax - barrier) * Math.pow(u, clusteringExponent);
		}

		grid[0] = sMin;
		grid[barrierIndex] = barrier;
		grid[numberOfSteps] = sMax;

		enforceStrictMonotonicity(grid);

		return new ArrayGrid(grid);
	}

	/**
	 * Returns the index of the barrier node in a grid previously built by this
	 * factory.
	 *
	 * @param grid The grid array.
	 * @param barrier The barrier level.
	 * @return The barrier node index.
	 */
	public static int findBarrierIndex(final double[] grid, final double barrier) {
		for (int i = 0; i < grid.length; i++) {
			if (Math.abs(grid[i] - barrier) < 1E-12) {
				return i;
			}
		}
		throw new IllegalArgumentException("Barrier is not a grid node.");
	}

	/**
	 * Convenience overload.
	 *
	 * @param grid The grid.
	 * @param barrier The barrier level.
	 * @return The barrier node index.
	 */
	public static int findBarrierIndex(final Grid grid, final double barrier) {
		return findBarrierIndex(grid.getGrid(), barrier);
	}

	/**
	 * Chooses the barrier node index so that the node count allocation roughly
	 * matches the geometric position of the barrier inside the interval.
	 * @param numberOfSteps The value.
	 * @param sMin The value.
	 * @param sMax The value.
	 * @param barrier The value.
	 * @return The value.
	 */
	private static int chooseBarrierIndex(
			final int numberOfSteps,
			final double sMin,
			final double sMax,
			final double barrier) {

		final double relativePosition = (barrier - sMin) / (sMax - sMin);
		int barrierIndex = (int)Math.round(relativePosition * numberOfSteps);

		/*
		 * Force strict interior placement.
		 */
		barrierIndex = Math.max(1, barrierIndex);
		barrierIndex = Math.min(numberOfSteps - 1, barrierIndex);

		return barrierIndex;
	}

	private static void validateInputs(
			final int numberOfSteps,
			final double sMin,
			final double sMax,
			final double barrier) {

		if (numberOfSteps < 2) {
			throw new IllegalArgumentException("numberOfSteps must be at least 2.");
		}
		if (!(sMin < sMax)) {
			throw new IllegalArgumentException("Require sMin < sMax.");
		}
		if (!(barrier > sMin && barrier < sMax)) {
			throw new IllegalArgumentException("Barrier must lie strictly inside (sMin, sMax).");
		}
	}

	/**
	 * Guards against duplicate nodes caused by extreme clustering and floating-
	 * point rounding.
	 * @param grid The value.
	 */
	private static void enforceStrictMonotonicity(final double[] grid) {
		final double epsilon = 1E-12;

		for (int i = 1; i < grid.length; i++) {
			if (grid[i] <= grid[i - 1]) {
				grid[i] = grid[i - 1] + epsilon;
			}
		}
	}

	/**
	 * Simple immutable array-backed grid.
	 */
	private static final class ArrayGrid extends AbstractGrid {

		/**
		 * The grid.
		 */
		private final double[] grid;
		/**
		 * The delta.
		 */
		private double[] delta;

		private ArrayGrid(final double[] grid) {
			this.grid = Arrays.copyOf(grid, grid.length);
		}

		@Override
		public double[] getGrid() {
			return Arrays.copyOf(grid, grid.length);
		}

		@Override
		public double[] getDelta(final double[] valuesOnGrid) {

			if (delta == null) {
				if (valuesOnGrid == null) {
					throw new IllegalArgumentException("valuesOnGrid must not be null.");
				}
				if (valuesOnGrid.length != grid.length) {
					throw new IllegalArgumentException(
							"valuesOnGrid length must match grid length."
							);
				}

				delta = new double[valuesOnGrid.length - 1];
				for (int i = 0; i < valuesOnGrid.length - 1; i++) {
					delta[i] = valuesOnGrid[i + 1] - valuesOnGrid[i];
				}
			}

			return Arrays.copyOf(delta, delta.length);
		}
	}
}
