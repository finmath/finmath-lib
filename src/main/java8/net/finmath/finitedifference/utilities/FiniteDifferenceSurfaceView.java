package net.finmath.finitedifference.utilities;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.grids.SpaceTimeDiscretization;

/**
 * Lightweight view on a finite-difference value surface.
 *
 * <p>
 * The view couples a raw finite-difference value surface with its
 * {@link SpaceTimeDiscretization}. It provides indexed access, spatial
 * interpolation, and dependency-free function adapters for plotting.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class FiniteDifferenceSurfaceView {

	/**
	 * The default number of plot points.
	 */
	private static final int DEFAULT_NUMBER_OF_PLOT_POINTS = 100;

	/**
	 * The discretization.
	 */
	private final SpaceTimeDiscretization discretization;
	/**
	 * The values.
	 */
	private final double[][] values;
	/**
	 * The layout.
	 */
	private final FiniteDifferenceGridLayout layout;

	/**
	 * Creates a surface view.
	 *
	 * @param discretization The space-time discretization.
	 * @param values The value surface indexed by flattened spatial index and
	 *     time index.
	 */
	public FiniteDifferenceSurfaceView(
			final SpaceTimeDiscretization discretization,
			final double[][] values) {

		this.layout = FiniteDifferenceGridLayout.of(discretization);
		layout.validateSurface(values);

		this.discretization = discretization;
		this.values = values;
	}

	/**
	 * Performs the operation.
	 *
	 * @param discretization The value.
	 * @param values The value.
	 * @return The value.
	 */
	public static FiniteDifferenceSurfaceView of(
			final SpaceTimeDiscretization discretization,
			final double[][] values) {
		return new FiniteDifferenceSurfaceView(discretization, values);
	}

	/**
	 * Returns the value.
	 *
	 * @param timeIndex The value.
	 * @param indices The value.
	 * @return The value.
	 */
	public double getValue(final int timeIndex, final int... indices) {
		validateTimeIndex(timeIndex);
		return values[layout.flatten(indices)][timeIndex];
	}

	/**
	 * Performs the operation.
	 *
	 * @param timeIndex The value.
	 * @param coordinates The value.
	 * @return The value.
	 */
	public double interpolate(final int timeIndex, final double... coordinates) {
		validateTimeIndex(timeIndex);
		return FiniteDifferenceValueInterpolator.interpolateTimeIndex(
				values,
				discretization,
				timeIndex,
				coordinates
		);
	}

	/**
	 * Performs the operation.
	 *
	 * @param evaluationTime The value.
	 * @param maturity The value.
	 * @param coordinates The value.
	 * @return The value.
	 */
	public double interpolate(
			final double evaluationTime,
			final double maturity,
			final double... coordinates) {
		return FiniteDifferenceValueInterpolator.interpolateSurface(
				values,
				discretization,
				evaluationTime,
				maturity,
				coordinates
		);
	}

	/**
	 * Returns the value.
	 *
	 * @param timeIndex The value.
	 * @return The value.
	 */
	public double[] getTimeSlice(final int timeIndex) {
		validateTimeIndex(timeIndex);
		return FiniteDifferenceValueInterpolator.getTimeSlice(
				values,
				discretization,
				timeIndex
		);
	}

	/**
	 * Performs the operation.
	 *
	 * @param timeIndex The value.
	 * @return The value.
	 */
	public DoubleUnaryOperator asFunction1D(final int timeIndex) {
		if (layout.getDimension() != 1) {
			throw new IllegalArgumentException("asFunction1D requires a one-dimensional surface.");
		}

		validateTimeIndex(timeIndex);

		return x -> interpolate(timeIndex, x);
	}

	/**
	 * Performs the operation.
	 *
	 * @param timeIndex The value.
	 * @param xDimension The value.
	 * @param yDimension The value.
	 * @param fixedCoordinates The value.
	 * @return The value.
	 */
	public DoubleBinaryOperator asFunction2D(
			final int timeIndex,
			final int xDimension,
			final int yDimension,
			final double... fixedCoordinates) {

		validateTimeIndex(timeIndex);
		validatePlotDimensions(xDimension, yDimension);

		final double[] baseCoordinates = createBaseCoordinates(fixedCoordinates);

		return (x, y) -> {
			final double[] coordinates = baseCoordinates.clone();
			coordinates[xDimension] = x;
			coordinates[yDimension] = y;
			return interpolate(timeIndex, coordinates);
		};
	}

	/**
	 * Performs the operation.
	 *
	 * @param timeIndex The value.
	 * @param numberOfPoints The value.
	 * @param title The value.
	 * @param xAxisLabel The value.
	 * @param yAxisLabel The value.
	 * @return The value.
	 */
	public FiniteDifferencePlotData1D asPlotData1D(
			final int timeIndex,
			final int numberOfPoints,
			final String title,
			final String xAxisLabel,
			final String yAxisLabel) {

		if (layout.getDimension() != 1) {
			throw new IllegalArgumentException("asPlotData1D requires a one-dimensional surface.");
		}

		final double[] grid = discretization.getSpaceGrid(0).getGrid();

		return new FiniteDifferencePlotData1D(
				grid[0],
				grid[grid.length - 1],
				numberOfPoints,
				asFunction1D(timeIndex),
				title,
				xAxisLabel,
				yAxisLabel
		);
	}

	/**
	 * Performs the operation.
	 *
	 * @param timeIndex The value.
	 * @return The value.
	 */
	public FiniteDifferencePlotData1D asPlotData1D(final int timeIndex) {
		return asPlotData1D(
				timeIndex,
				DEFAULT_NUMBER_OF_PLOT_POINTS,
				"",
				"x",
				"value"
		);
	}

	/**
	 * Performs the operation.
	 *
	 * @param timeIndex The value.
	 * @param xDimension The value.
	 * @param yDimension The value.
	 * @param numberOfPointsX The value.
	 * @param numberOfPointsY The value.
	 * @param title The value.
	 * @param xAxisLabel The value.
	 * @param yAxisLabel The value.
	 * @param zAxisLabel The value.
	 * @param fixedCoordinates The value.
	 * @return The value.
	 */
	public FiniteDifferencePlotData2D asPlotData2D(
			final int timeIndex,
			final int xDimension,
			final int yDimension,
			final int numberOfPointsX,
			final int numberOfPointsY,
			final String title,
			final String xAxisLabel,
			final String yAxisLabel,
			final String zAxisLabel,
			final double... fixedCoordinates) {

		validateTimeIndex(timeIndex);
		validatePlotDimensions(xDimension, yDimension);

		final double[] xGrid = discretization.getSpaceGrid(xDimension).getGrid();
		final double[] yGrid = discretization.getSpaceGrid(yDimension).getGrid();

		return new FiniteDifferencePlotData2D(
				xGrid[0],
				xGrid[xGrid.length - 1],
				yGrid[0],
				yGrid[yGrid.length - 1],
				numberOfPointsX,
				numberOfPointsY,
				asFunction2D(timeIndex, xDimension, yDimension, fixedCoordinates),
				title,
				xAxisLabel,
				yAxisLabel,
				zAxisLabel
		);
	}

	/**
	 * Performs the operation.
	 *
	 * @param timeIndex The value.
	 * @param xDimension The value.
	 * @param yDimension The value.
	 * @return The value.
	 */
	public FiniteDifferencePlotData2D asPlotData2D(
			final int timeIndex,
			final int xDimension,
			final int yDimension) {

		return asPlotData2D(
				timeIndex,
				xDimension,
				yDimension,
				DEFAULT_NUMBER_OF_PLOT_POINTS,
				DEFAULT_NUMBER_OF_PLOT_POINTS,
				"",
				"x",
				"y",
				"value"
		);
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public SpaceTimeDiscretization getDiscretization() {
		return discretization;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double[][] getValues() {
		return values;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public FiniteDifferenceGridLayout getLayout() {
		return layout;
	}

	private double[] createBaseCoordinates(final double... fixedCoordinates) {
		final double[] baseCoordinates = new double[layout.getDimension()];

		for (int i = 0; i < baseCoordinates.length; i++) {
			baseCoordinates[i] = discretization.getCenter(i);
		}

		if (fixedCoordinates != null && fixedCoordinates.length > 0) {
			if (fixedCoordinates.length != layout.getDimension()) {
				throw new IllegalArgumentException(
						"fixedCoordinates must either be empty or have one entry per spatial dimension."
				);
			}

			for (int i = 0; i < fixedCoordinates.length; i++) {
				baseCoordinates[i] = fixedCoordinates[i];
			}
		}

		return baseCoordinates;
	}

	private void validatePlotDimensions(final int xDimension, final int yDimension) {
		if (xDimension == yDimension) {
			throw new IllegalArgumentException("xDimension and yDimension must be different.");
		}
		if (xDimension < 0 || xDimension >= layout.getDimension()) {
			throw new IllegalArgumentException("xDimension out of range.");
		}
		if (yDimension < 0 || yDimension >= layout.getDimension()) {
			throw new IllegalArgumentException("yDimension out of range.");
		}
	}

	private void validateTimeIndex(final int timeIndex) {
		if (values.length == 0 || values[0].length == 0) {
			throw new IllegalArgumentException("The surface contains no time indices.");
		}
		if (timeIndex < 0 || timeIndex >= values[0].length) {
			throw new IllegalArgumentException("timeIndex out of range.");
		}
	}
}
