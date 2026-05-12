package net.finmath.finitedifference.utilities;

import java.util.function.DoubleUnaryOperator;

/**
 * Dependency-free plot descriptor for a one-dimensional finite-difference
 * curve.
 *
 * <p>
 * This class intentionally depends only on standard Java functional interfaces.
 * Plotting libraries may consume the returned {@link DoubleUnaryOperator}
 * without creating a compile-time dependency from the finite-difference module
 * to a plotting module.
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class FiniteDifferencePlotData1D {

	/**
	 * The x min.
	 */
	private final double xMin;
	/**
	 * The x max.
	 */
	private final double xMax;
	/**
	 * The number of points.
	 */
	private final int numberOfPoints;
	/**
	 * The function.
	 */
	private final DoubleUnaryOperator function;
	/**
	 * The title.
	 */
	private final String title;
	/**
	 * The x axis label.
	 */
	private final String xAxisLabel;
	/**
	 * The y axis label.
	 */
	private final String yAxisLabel;

	/**
	 * Creates a one-dimensional plot descriptor.
	 *
	 * @param xMin The lower x-axis bound.
	 * @param xMax The upper x-axis bound.
	 * @param numberOfPoints The number of plot points.
	 * @param function The function to plot.
	 * @param title The plot title.
	 * @param xAxisLabel The x-axis label.
	 * @param yAxisLabel The y-axis label.
	 */
	public FiniteDifferencePlotData1D(
			final double xMin,
			final double xMax,
			final int numberOfPoints,
			final DoubleUnaryOperator function,
			final String title,
			final String xAxisLabel,
			final String yAxisLabel) {

		if (numberOfPoints <= 0) {
			throw new IllegalArgumentException("numberOfPoints must be positive.");
		}
		if (function == null) {
			throw new IllegalArgumentException("function must not be null.");
		}

		this.xMin = xMin;
		this.xMax = xMax;
		this.numberOfPoints = numberOfPoints;
		this.function = function;
		this.title = title;
		this.xAxisLabel = xAxisLabel;
		this.yAxisLabel = yAxisLabel;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getXMin() {
		return xMin;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public double getXMax() {
		return xMax;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public int getNumberOfPoints() {
		return numberOfPoints;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public DoubleUnaryOperator getFunction() {
		return function;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public String getXAxisLabel() {
		return xAxisLabel;
	}

	/**
	 * Returns the value.
	 *
	 * @return The value.
	 */
	public String getYAxisLabel() {
		return yAxisLabel;
	}
}
