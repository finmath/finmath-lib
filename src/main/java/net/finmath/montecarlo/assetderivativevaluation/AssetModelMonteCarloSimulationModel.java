/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Basic interface which has to be implemented by Monte Carlo models for asset processes.
 *
 * This interface serves as an abstraction layer between models and simulations (providing
 * the implementation of this interface) and products (using an object implementing this interface
 * in its valuation code, without further knowledge of the specific model and simulation technique.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface AssetModelMonteCarloSimulationModel extends MonteCarloSimulationModel {

	/**
	 * Returns the number of asset price processes.
	 *
	 * @return The number of asset price processes
	 */
	int getNumberOfAssets();

	/**
	 * Returns the random variable representing the asset's value at a given time for a given asset.
	 *
	 * @param timeIndex		Index of simulation time
	 * @param assetIndex	Index of the asset (0 for a single asset model)
	 * @return				The asset process as seen on simulation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable		getAssetValue(int timeIndex, int assetIndex) throws CalculationException;

	/**
	 * Returns the random variable representing the asset's value at a given time for a given asset.
	 *
	 * @param time			Simulation time
	 * @param assetIndex	Index of the asset (0 for a single asset model)
	 * @return				The asset process as seen on simulation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable		getAssetValue(double time, int assetIndex) throws CalculationException;

	/**
	 * Returns the numeraire associated with the valuation measure used by this model.
	 *
	 * @param timeIndex The time index (associated with this models time discretization).
	 * @return The numeraire associated with the valuation measure used by this model.
	 * @throws CalculationException Thrown if calculation of numeraire fails.
	 */
	RandomVariable		getNumeraire(int timeIndex) throws CalculationException;

	/**
	 * Returns the numeraire associated with the valuation measure used by this model.
	 *
	 * @param time The time for which the numeraire is requested.
	 * @return The numeraire associated with the valuation measure used by this model.
	 * @throws CalculationException Thrown if calculation of numeraire fails.
	 */
	RandomVariable		getNumeraire(double time) throws CalculationException;

	/**
	 * Create a clone of this simulation modifying some of its properties (if any).
	 *
	 * @param dataModified The data which should be changed in the new model
	 * @return Returns a clone of this model, with some data modified (then it is no longer a clone :-)
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	AssetModelMonteCarloSimulationModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;

	/**
	 * Create a clone of the object implementing <code>AssetModelMonteCarloSimulationModel</code>
	 * using a different Monte-Carlo seed.
	 *
	 * @param seed The seed of the underlying random number generator.
	 * @return Returns a clone of this model except for a modified Monte-Carlo seed.
	 * @throws CalculationException Thrown if cloning failed.
	 */
	AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(int seed)  throws CalculationException;
}
