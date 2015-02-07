/*
 * Created on 12.08.2012
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.timeseries;


/**
 * A parametric time series model
 * 
 * @author Christian Fries
 */
public interface TimeSeriesModelParametric {

	double[] getParameters();

	String[] getParameterNames();

	TimeSeriesModelParametric getCloneCalibrated(TimeSeriesInterface timeSeries);
}