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
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.process.component.factortransform.FactorTransform;
import net.finmath.stochastic.RandomVariable;

/**
 * Base class for products requiring an TermStructureMonteCarloSimulationModel (or LIBORModelMonteCarloSimulationModel) as base class for the valuation model argument
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractTermStructureMonteCarloProduct extends AbstractMonteCarloProduct implements TermStructureMonteCarloProduct {

	/**
	 * @param currency The currency of this product (may be null for "any currency").
	 */
	public AbstractTermStructureMonteCarloProduct(final String currency) {
		super(currency);
	}

	/**
	 *
	 */
	public AbstractTermStructureMonteCarloProduct() {
		super(null);
	}

	@Override
	public abstract RandomVariable getValue(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException;

	public RandomVariable getValueForModifiedData(final double evaluationTime, final MonteCarloSimulationModel monteCarloSimulationModel, final Map<String, Object> dataModified) throws CalculationException
	{
		return this.getValue(evaluationTime, monteCarloSimulationModel.getCloneWithModifiedData(dataModified));
	}

	@Override
	public Map<String, Object> getValues(final double evaluationTime, final TermStructureMonteCarloSimulationModel model) throws CalculationException {
		final RandomVariable value = getValue(evaluationTime, model);
		final Map<String, Object> result = new HashMap<>();
		result.put("value", value.getAverage());
		result.put("error", value.getStandardError());
		return result;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final MonteCarloSimulationModel model) throws CalculationException {
		// This product requires an TermStructureMonteCarloSimulationModel model, otherwise there will be a class cast exception
		if(model == null || model instanceof TermStructureMonteCarloSimulationModel) {
			return getValue(evaluationTime, (TermStructureMonteCarloSimulationModel)model);
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + TermStructureMonteCarloSimulationModel.class + ".");
		}
	}

	@Override
	public FactorTransform getFactorDrift(final LIBORModelMonteCarloSimulationModel referenceScheme, final LIBORModelMonteCarloSimulationModel targetScheme) {
		throw new UnsupportedOperationException("Method not implemented.");
	}
}
