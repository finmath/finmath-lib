package net.finmath.timeseries;

import java.util.Iterator;

/**
 * A discrete time series.
 *
 * @author Christian Fries
 */
public class TimeSeries implements TimeSeriesInterface {

	private final double[] times;
	private final double[] values;

	public TimeSeries(double[] times, double[] values) {
		super();
		this.times = times;
		this.values = values;
	}

	@Override
	public double getTime(int index) {
		return times[index];
	}

	@Override
	public double getValue(int index) {
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
						return index < TimeSeries.this.getNumberOfTimePoints();
					}

					@Override
					public Double next() {
						return TimeSeries.this.getValue(index++);
					}

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

		};
	}

}
