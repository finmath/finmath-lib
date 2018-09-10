/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 12.08.2012
 */
package net.finmath.timeseries;

import java.util.Map;

/**
 * A parametric time series model based on a given times series.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface HistoricalSimulationModel {

	/**
	 * Create a new model, using only a window of the times series.
	 *
	 * @param windowIndexStart Index of the first element to be part of the new time series.
	 * @param windowIndexEnd Index of the last element to be part of the new time series.
	 * @return A new historical simulation using a different data window.
	 */
	HistoricalSimulationModel getCloneWithWindow(int windowIndexStart, int windowIndexEnd);

	/**
	 * Returns the parameters estimated for the given time series.
	 *
	 * @return The parameters estimated for the given time series.
	 */
	Map<String, Object> getBestParameters();

	/**
	 * Returns the parameters estimated for the given time series, using a parameter guess.
	 *
	 * @param previousResults A parameter guess.
	 * @return The parameters estimated for the given time series.
	 */
	Map<String, Object> getBestParameters(Map<String, Object> previousResults);
}
