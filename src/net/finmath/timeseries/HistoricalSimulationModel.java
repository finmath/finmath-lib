/*
 * Created on 12.08.2012
 *
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 */
package net.finmath.timeseries;

import java.util.Map;

/**
 * A parametric time series model based on a given times series.
 * 
 * @author Christian Fries
 */
public interface HistoricalSimulationModel {

	/**
	 * Create a new model, using only a window of the times series.
	 * 
	 * @param windowIndexStart Index of the first element to be part of the new time series.
	 * @param windowIndexEnd Index of the last element to be part of the new time series.
	 * @return
	 */
	HistoricalSimulationModel getCloneWithWindow(int windowIndexStart, int windowIndexEnd);

	/**
	 * Returns the parameters estimated for the given time series.
	 * 
	 * @return The parameters estimated for the given time series.
	 */
	Map<String, Double> getBestParameters();

	/**
	 * Returns the parameters estimated for the given time series, using a parameter guess.
	 * 
	 * @param guess A parameter guess.
	 * @return The parameters estimated for the given time series.
	 */
	Map<String, Double> getBestParameters(Map<String, Double> guess);
}