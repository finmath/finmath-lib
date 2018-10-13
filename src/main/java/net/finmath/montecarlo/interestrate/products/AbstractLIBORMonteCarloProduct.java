/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.process.component.factordrift.FactorDriftInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base calls for product that need an AbstractLIBORMarketModel as base class
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractLIBORMonteCarloProduct extends AbstractMonteCarloProduct {

	/**
	 * @param currency The currency of this product (may be null for "any currency").
	 */
	public AbstractLIBORMonteCarloProduct(String currency) {
		super(currency);
	}

	/**
	 *
	 */
	public AbstractLIBORMonteCarloProduct() {
		super(null);
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public abstract RandomVariableInterface getValue(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException;

	public RandomVariableInterface getValueForModifiedData(double evaluationTime, MonteCarloSimulationInterface monteCarloSimulationInterface, Map<String, Object> dataModified) throws CalculationException
	{
		return this.getValue(evaluationTime, monteCarloSimulationInterface.getCloneWithModifiedData(dataModified));
	}

	/**
	 * This method returns the valuation of the product within the specified model, evaluated at a given evalutationTime.
	 * The valuation is returned in terms of a map. The map may contain additional information.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public Map<String, Object> getValues(double evaluationTime, LIBORModelMonteCarloSimulationInterface model) throws CalculationException {
		RandomVariableInterface value = getValue(evaluationTime, model);
		Map<String, Object> result = new HashMap<>();
		result.put("value", value.getAverage());
		result.put("error", value.getStandardError());
		return result;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException {
		// This product requires an LIBORModelMonteCarloSimulationInterface model, otherwise there will be a class cast exception
		if(model instanceof LIBORModelMonteCarloSimulationInterface) {
			return getValue(evaluationTime, (LIBORModelMonteCarloSimulationInterface)model);
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + LIBORModelMonteCarloSimulationInterface.class + ".");
		}
	}

	/**
	 * Overwrite this method if the product supplies a custom FactorDriftInterface to be used in proxy simulation.
	 *
	 * @param referenceScheme The reference scheme
	 * @param targetScheme The target scheme
	 * @return The FactorDriftInterface
	 */
	public FactorDriftInterface getFactorDrift(LIBORModelMonteCarloSimulationInterface referenceScheme, LIBORModelMonteCarloSimulationInterface targetScheme) {
		return null;
	}
}
