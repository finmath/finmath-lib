/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.modelling.ModelInterface;
import net.finmath.modelling.ProductInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base class for products requiring an MonteCarloSimulationInterface for valuation.
 * 
 * @author Christian Fries
 */
public abstract class AbstractMonteCarloProduct implements ProductInterface {

	private final String currency;

	public AbstractMonteCarloProduct(String currency) {
		super();
		this.currency = currency;
	}

	public AbstractMonteCarloProduct() {
		this(null);
	}

	@Override
	public Object getValue(double evaluationTime, ModelInterface model) {
		throw new IllegalArgumentException("The product " + this.getClass()
				+ " cannot be valued against a model " + model.getClass() + "."
				+ "It requires a model of type " + AnalyticModelInterface.class + ".");
	}

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
	public abstract RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException;


	/**
	 * This method returns the value of the product under the specified model. 
	 * 
	 * @param model A model used to evaluate the product.
	 * @return The value of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public double getValue(MonteCarloSimulationInterface model) throws CalculationException {
		return getValue(0.0, model).getAverage();
	}

	/**
	 * This method returns the value of the product under the specified model and other information in a key-value map. 
	 * 
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model A model used to evaluate the product.
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public Map<String, Object> getValues(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException
	{
		RandomVariableInterface values = getValue(evaluationTime, model);

		if(values == null) return null;

		// Sum up values on path
		double value = values.getAverage();
		double error = values.getStandardError();

		Map<String, Object> results = new HashMap<String, Object>();
		results.put("value", value);
		results.put("error", error);

		return results;
	}

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
	public Map<String, Object> getValuesForModifiedData(double evaluationTime, MonteCarloSimulationInterface model, Map<String,Object> dataModified) throws CalculationException
	{
		MonteCarloSimulationInterface modelModified = model.getCloneWithModifiedData(dataModified);

		return getValues(evaluationTime, modelModified);
	}

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
	public Map<String, Object> getValuesForModifiedData(double evaluationTime, MonteCarloSimulationInterface model, String entityKey, Object dataModified) throws CalculationException
	{
		Map<String, Object> dataModifiedMap = new HashMap<String, Object>();
		dataModifiedMap.put(entityKey, dataModified);
		return getValuesForModifiedData(evaluationTime, model, dataModifiedMap);
	}

	/**
	 * This method returns the value of the product under the specified model and other information in a key-value map. 
	 * 
	 * @param model A model used to evaluate the product.
	 * @return The values of the product.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public Map<String, Object> getValues(MonteCarloSimulationInterface model) throws CalculationException
	{
		return getValues(0.0, model);
	}

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
	public Map<String, Object> getValuesForModifiedData(MonteCarloSimulationInterface model, Map<String,Object> dataModified) throws CalculationException
	{
		return getValuesForModifiedData(0.0, model, dataModified);
	}

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
	public Map<String, Object> getValuesForModifiedData(MonteCarloSimulationInterface model, String entityKey, Object dataModified) throws CalculationException
	{
		return getValuesForModifiedData(0.0, model, entityKey, dataModified);
	}

	/**
	 * Returns the currency string of this notional.
	 * 
	 * @return the currency
	 */
	public String getCurrency() {
		return currency;
	}

	@Override
	public String toString() {
		return "AbstractMonteCarloProduct [currency=" + currency + "]";
	}
}
