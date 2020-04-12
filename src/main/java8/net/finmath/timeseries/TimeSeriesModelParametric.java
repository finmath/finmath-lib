/*
 * Created on 12.08.2012
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.timeseries;


/**
 * A parametric time series model
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface TimeSeriesModelParametric {

	double[] getParameters();

	String[] getParameterNames();

	TimeSeriesModelParametric getCloneCalibrated(TimeSeries timeSeries);
}
