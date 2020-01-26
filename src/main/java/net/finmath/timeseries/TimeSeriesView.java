package net.finmath.timeseries;

import java.util.Iterator;

/**
 * A time series created from a sup-interval of another time series.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class TimeSeriesView implements TimeSeries {

	private final TimeSeries timeSeries;
	private final int indexStart;
	private final int indexEnd;

	public TimeSeriesView(final TimeSeries timeSeries, final int indexStart, final int indexEnd) {
		super();
		this.timeSeries = timeSeries;
		this.indexStart = indexStart;
		this.indexEnd = indexEnd;
	}

	@Override
	public double getTime(final int index) {
		return timeSeries.getTime(indexStart+index);
	}

	@Override
	public double getValue(final int index) {
		return timeSeries.getValue(indexStart+index);
	}

	@Override
	public int getNumberOfTimePoints() {
		return indexEnd-indexStart+1;
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
						return index < TimeSeriesView.this.getNumberOfTimePoints();
					}

					@Override
					public Double next() {
						return TimeSeriesView.this.getValue(index++);
					}
				};
			}

		};
	}
}
