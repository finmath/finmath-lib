/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.05.2006
 */
package net.finmath.montecarlo;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base class for product needing an MonteCarloSimulationInterface
 * 
 * @author Christian Fries
 */
public abstract class AbstractMonteCarloProduct {

	public AbstractMonteCarloProduct() {
		super();
	}

	/**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws CalculationException 
     */
    public abstract RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException;

    /**
     * This method returns the value of the product under the specified model. 
     * 
     * @param model A model used to evaluate the product.
     * @return The value of the product.
     * @throws CalculationException 
     */
    public double getValue(MonteCarloSimulationInterface model) throws CalculationException {

    	Map<String, Object> value = getValues(0.0, model);
        
        return ((Double)value.get("value")).doubleValue();
    }
    
    /**
     * This method returns the value of the product under the specified model and other information in a key-value map. 
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model A model used to evaluate the product.
     * @return The values of the product.
     * @throws CalculationException 
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
     * @param model The model used to price the product, except for the market data to modify
	 * @param dataModified The new market data object to use (could be of different types)
	 * 
     * @return The values of the product.
	 * @throws CalculationException 
     */
	public Map<String, Object> getValuesForModifiedData(MonteCarloSimulationInterface model, Map<String,Object> dataModified) throws CalculationException
	{
		MonteCarloSimulationInterface modelModified = (MonteCarloSimulationInterface)model.getCloneWithModifiedData(dataModified);

		return getValues(0.0, modelModified);
	}
}
