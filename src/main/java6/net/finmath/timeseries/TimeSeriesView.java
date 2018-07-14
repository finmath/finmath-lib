package net.finmath.timeseries;

import java.util.Iterator;

public class TimeSeriesView implements TimeSeriesInterface {

	private final TimeSeriesInterface timeSeries;
	private final int indexStart;
	private final int indexEnd;

	public TimeSeriesView(TimeSeriesInterface timeSeries, int indexStart, int indexEnd) {
		super();
		this.timeSeries = timeSeries;
		this.indexStart = indexStart;
		this.indexEnd = indexEnd;
	}

	@Override
	public double getTime(int index) {
		return timeSeries.getTime(indexStart+index);
	}

	@Override
	public double getValue(int index) {
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

					@Override
					public void remove() {
						throw new UnsupportedOperationException();
					}
				};
			}

		};
	}
}

