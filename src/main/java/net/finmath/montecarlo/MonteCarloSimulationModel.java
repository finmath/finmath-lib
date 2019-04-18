/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2005
 */
package net.finmath.montecarlo;

import java.time.LocalDateTime;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.Model;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * The interface implemented by a simulation of an SDE.
 * Provides the dimension of the SDE and the the time discretization of the
 * simulation.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface MonteCarloSimulationModel extends Model {

	/**
	 * Returns the numberOfPaths.
	 *
	 * @return Returns the numberOfPaths.
	 */
	int getNumberOfPaths();

	/**
	 * Returns the model's date corresponding to the time discretization's \( t = 0 \).
	 *
	 * @return The model's date corresponding to the time discretization's \( t = 0 \).
	 */
	default LocalDateTime getReferenceDate() {
		// TODO remove default value in 4.0.0 - this is only for backward compatiblity.
		return null;
	}

	/**
	 * Returns the timeDiscretizationFromArray.
	 *
	 * @return Returns the timeDiscretizationFromArray.
	 */
	TimeDiscretization getTimeDiscretization();

	/**
	 * Returns the time for a given time index.
	 *
	 * @param timeIndex Time index
	 * @return Returns the time for a given time index.
	 */
	double getTime(int timeIndex);

	/**
	 * Returns the time index for a given time.
	 *
	 * @param time The time.
	 * @return Returns the time index for a given time.
	 */
	int getTimeIndex(double time);

	/**
	 * Returns a random variable which is initialized to a constant,
	 * but has exactly the same number of paths or discretization points as the ones used by this <code>MonteCarloSimulationModel</code>.
	 *
	 * @param value The constant value to be used for initialized the random variable.
	 * @return A new random variable.
	 */
	RandomVariable getRandomVariableForConstant(double value);

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 *
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getMonteCarloWeights(int timeIndex) throws CalculationException;

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 *
	 * @param time Time at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getMonteCarloWeights(double time) throws CalculationException;

	/**
	 * Create a clone of this simulation modifying some of its properties (if any).
	 *
	 * The properties that should be modified correspond to arguments of constructors. A constructor is then called
	 * with where all arguments that are not found in the key value map are being set to this objects values.
	 *
	 * @param dataModified The data which should be changed in the new model. This is a key value may, where the key corresponds to the name of a property in one of the objects constructors.
	 * @return Returns a clone of this object, with some data modified (then it is no longer a clone :-)
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	MonteCarloSimulationModel getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException;
}
