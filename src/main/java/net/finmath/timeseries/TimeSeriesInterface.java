package net.finmath.timeseries;


public interface TimeSeriesInterface {

	double getTime(int index);
	double getValue(int index);

	int getNumberOfTimePoints();

	Iterable<Double> getValues();
}

