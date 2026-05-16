package net.finmath.finitedifference.solvers.adi;

import net.finmath.time.TimeDiscretization;

/**
 * Immutable container for the activated post-hit value traced along the barrier
 * as a function of the second state variable and time.
 *
 * <p>
 * The intended usage is the direct 2D knock-in formulation:
 * first solve the activated vanilla problem, then extract its value on the
 * barrier and use that trace as interface data for the pre-hit PDE.
 * </p>
 *
 * <p>
 * The data layout is values[secondIndex][timeIndex].
 * </p>
 *
 * @author Alessandro Gnoatto
 */
public final class ActivatedBarrierTrace2D {

	/**
	 * The tolerance.
	 */
	private static final double TOLERANCE = 1E-12;

	/**
	 * The barrier value.
	 */
	private final double barrierValue;
	/**
	 * The second state grid.
	 */
	private final double[] secondStateGrid;
	/**
	 * The time grid.
	 */
	private final TimeDiscretization timeGrid;
	/**
	 * The values.
	 */
	private final double[][] values;

	/**
	 * Creates an immutable barrier trace container.
	 *
	 * @param barrierValue The barrier level in the first state variable.
	 * @param secondStateGrid The grid of the second state variable.
	 * @param timeGrid The time discretization corresponding to the trace
	 *     values.
	 * @param values Trace values with layout values[secondIndex][timeIndex].
	 */
	public ActivatedBarrierTrace2D(
			final double barrierValue,
			final double[] secondStateGrid,
			final TimeDiscretization timeGrid,
			final double[][] values) {

		if (secondStateGrid == null || secondStateGrid.length == 0) {
			throw new IllegalArgumentException("secondStateGrid must not be null or empty.");
		}
		if (timeGrid == null) {
			throw new IllegalArgumentException("timeGrid must not be null.");
		}
		if (values == null || values.length != secondStateGrid.length) {
			throw new IllegalArgumentException(
					"values must have one row for each second-state grid point.");
		}

		final int expectedTimeCount = timeGrid.getNumberOfTimes();
		for (int i = 0; i < values.length; i++) {
			if (values[i] == null || values[i].length != expectedTimeCount) {
				throw new IllegalArgumentException(
						"Each values row must have length equal to timeGrid.getNumberOfTimes().");
			}
		}

		this.barrierValue = barrierValue;
		this.secondStateGrid = secondStateGrid.clone();
		this.timeGrid = timeGrid;
		this.values = deepCopy(values);
	}

	/**
	 * Returns the barrier level.
	 *
	 * @return The barrier value.
	 */
	public double getBarrierValue() {
		return barrierValue;
	}

	/**
	 * Returns a defensive copy of the second-state grid.
	 *
	 * @return The second-state grid.
	 */
	public double[] getSecondStateGrid() {
		return secondStateGrid.clone();
	}

	/**
	 * Returns the time grid.
	 *
	 * @return The time discretization.
	 */
	public TimeDiscretization getTimeGrid() {
		return timeGrid;
	}

	/**
	 * Returns a defensive deep copy of the stored trace values.
	 *
	 * @return The full trace matrix with layout values[secondIndex][timeIndex].
	 */
	public double[][] getValues() {
		return deepCopy(values);
	}

	/**
	 * Returns the stored value for a given second-state row and time index.
	 *
	 * @param secondIndex The second-state index.
	 * @param timeIndex The time index.
	 * @return The trace value.
	 */
	public double getValue(final int secondIndex, final int timeIndex) {
		validateSecondIndex(secondIndex);
		validateTimeIndex(timeIndex);
		return values[secondIndex][timeIndex];
	}

	/**
	 * Returns the trace value for a given second-state row and time.
	 *
	 * <p>
	 * If tau lies exactly on a time node, that node value is returned.
	 * Otherwise linear interpolation in time is used between the enclosing
	 * nodes.
	 * Values outside the grid are clamped to the nearest endpoint.
	 * </p>
	 *
	 * @param secondIndex The second-state index.
	 * @param tau The time coordinate.
	 * @return The interpolated trace value.
	 */
	public double getValue(final int secondIndex, final double tau) {
		validateSecondIndex(secondIndex);

		final int numberOfTimes = timeGrid.getNumberOfTimes();
		if (numberOfTimes == 1) {
			return values[secondIndex][0];
		}

		final double firstTime = timeGrid.getTime(0);
		final double lastTime = timeGrid.getTime(numberOfTimes - 1);

		if (tau <= firstTime + TOLERANCE) {
			return values[secondIndex][0];
		}
		if (tau >= lastTime - TOLERANCE) {
			return values[secondIndex][numberOfTimes - 1];
		}

		final int upperIndex = timeGrid.getTimeIndexNearestGreaterOrEqual(tau);
		if (upperIndex < 0) {
			return values[secondIndex][numberOfTimes - 1];
		}

		final double upperTime = timeGrid.getTime(upperIndex);
		if (Math.abs(upperTime - tau) <= TOLERANCE) {
			return values[secondIndex][upperIndex];
		}

		final int lowerIndex = upperIndex - 1;
		if (lowerIndex < 0) {
			return values[secondIndex][upperIndex];
		}

		final double lowerTime = timeGrid.getTime(lowerIndex);
		final double lowerValue = values[secondIndex][lowerIndex];
		final double upperValue = values[secondIndex][upperIndex];

		final double weight = (tau - lowerTime) / (upperTime - lowerTime);
		return (1.0 - weight) * lowerValue + weight * upperValue;
	}

	/**
	 * Returns the number of second-state rows.
	 *
	 * @return The number of second-state grid points.
	 */
	public int getNumberOfSecondStatePoints() {
		return secondStateGrid.length;
	}

	/**
	 * Returns the number of time nodes.
	 *
	 * @return The number of time points.
	 */
	public int getNumberOfTimePoints() {
		return timeGrid.getNumberOfTimes();
	}

	private void validateSecondIndex(final int secondIndex) {
		if (secondIndex < 0 || secondIndex >= secondStateGrid.length) {
			throw new IndexOutOfBoundsException(
					"secondIndex out of bounds: " + secondIndex);
		}
	}

	private void validateTimeIndex(final int timeIndex) {
		if (timeIndex < 0 || timeIndex >= timeGrid.getNumberOfTimes()) {
			throw new IndexOutOfBoundsException(
					"timeIndex out of bounds: " + timeIndex);
		}
	}

	private static double[][] deepCopy(final double[][] source) {
		final double[][] copy = new double[source.length][];
		for (int i = 0; i < source.length; i++) {
			copy[i] = source[i].clone();
		}
		return copy;
	}
}
