package net.finmath.timeseries;

import java.util.Iterator;

/**
 * A discrete time series.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class TimeSeriesFromArray implements TimeSeries {

	private final double[] times;
	private final double[] values;

	public TimeSeriesFromArray(final double[] times, final double[] values) {
		super();
		this.times = times;
		this.values = values;
	}

	@Override
	public double getTime(final int index) {
		return times[index];
	}

	@Override
	public double getValue(final int index) {
		return values[index];
	}

	@Override
	public int getNumberOfTimePoints() {
		return times.length;
	}

	@Override
	public Iterable<Double> getValues() {
		return new Iterable<Double>() {
			private int index = 0;

			@Override
			public Iterator<Double> iterator() {
				return new Iterator<Double>() {
					@Override
					public boolean hasNext() {
						return index < TimeSeriesFromArray.this.getNumberOfTimePoints();
					}

					@Override
					public Double next() {
						return TimeSeriesFromArray.this.getValue(index++);
					}
				};
			}

		};
	}

}
