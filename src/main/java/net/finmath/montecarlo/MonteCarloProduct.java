/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.modelling.Model;
import net.finmath.modelling.Product;
import net.finmath.stochastic.RandomVariable;

/**
 * Interface for products requiring an MonteCarloSimulationModel for valuation.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface MonteCarloProduct extends Product {

	/**
	 * Returns the currency string of this product.
	 *
	 * @return the currency
	 */
	String getCurrency();

	@Override
	Object getValue(double evaluationTime, Model model);

	@Override
	Map<String, Object> getValues(double evaluationTime, Model model);

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 *
	 * For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 *
	 * More generally: The value random variable is a random variable <i>V<sup>*(t)</sup></i> such that
	 * the time-<i>t</i> conditional expectation of <i>V<sup>*(t)</sup></i> is equal
	 * to the value of the financial product in time <i>t</i>.
	 *
	 * An example for <i>V<sup>*(t)</sup></i> is the sum of <i>t</i>-discounted payoffs.
	 *
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable getValue(double evaluationTime, MonteCarloSimulationModel model) throws CalculationException;

	/**
	 * This method returns the value of the product under the specified model.
	 *
	 * @param model A model used to evaluate the product.
	 * @return The value of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	double getValue(MonteCarloSimulationModel model) throws CalculationException;

	/**
	 * This method returns the value of the product under the specified model and other information in a key-value map.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model A model used to evaluate the product.
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	Map<String, Object> getValues(double evaluationTime, MonteCarloSimulationModel model) throws CalculationException;

	/**
	 * This method returns the value under shifted market data (or model parameters).
	 * In its default implementation it does bump (creating a new model) and revalue.
	 * Override the way the new model is created, to implemented improved techniques (proxy scheme, re-calibration).
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product, except for the market data to modify
	 * @param dataModified The new market data object to use (could be of different types)
	 *
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	Map<String, Object> getValuesForModifiedData(double evaluationTime, MonteCarloSimulationModel model,
			Map<String, Object> dataModified) throws CalculationException;

	/**
	 * This method returns the value under shifted market data (or model parameters).
	 * In its default implementation it does bump (creating a new model) and revalue.
	 * Override the way the new model is created, to implemented improved techniques (proxy scheme, re-calibration).
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product, except for the market data to modify
	 * @param entityKey The entity to change, it depends on the model if the model reacts to this key.
	 * @param dataModified The new market data object to use (could be of different types)
	 *
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	Map<String, Object> getValuesForModifiedData(double evaluationTime, MonteCarloSimulationModel model,
			String entityKey, Object dataModified) throws CalculationException;

	/**
	 * This method returns the value of the product under the specified model and other information in a key-value map.
	 *
	 * @param model A model used to evaluate the product.
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	Map<String, Object> getValues(MonteCarloSimulationModel model) throws CalculationException;

	/**
	 * This method returns the value under shifted market data (or model parameters).
	 * In its default implementation it does bump (creating a new model) and revalue.
	 * Override the way the new model is created, to implemented improved techniques (proxy scheme, re-calibration).
	 *
	 * @param model The model used to price the product, except for the market data to modify
	 * @param dataModified The new market data object to use (could be of different types)
	 *
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	Map<String, Object> getValuesForModifiedData(MonteCarloSimulationModel model, Map<String, Object> dataModified)
			throws CalculationException;

	/**
	 * This method returns the value under shifted market data (or model parameters).
	 * In its default implementation it does bump (creating a new model) and revalue.
	 * Override the way the new model is created, to implemented improved techniques (proxy scheme, re-calibration).
	 *
	 * @param model The model used to price the product, except for the market data to modify
	 * @param entityKey The entity to change, it depends on the model if the model reacts to this key.
	 * @param dataModified The new market data object to use (could be of different types)
	 *
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	Map<String, Object> getValuesForModifiedData(MonteCarloSimulationModel model, String entityKey, Object dataModified)
			throws CalculationException;
}
