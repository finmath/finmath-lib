package net.finmath.timeseries;

/**
 * Interface to be implemented by finite time series.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface TimeSeries {

	double getTime(int index);
	double getValue(int index);

	int getNumberOfTimePoints();

	Iterable<Double> getValues();
}
