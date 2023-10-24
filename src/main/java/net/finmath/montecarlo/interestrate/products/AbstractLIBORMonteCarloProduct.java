/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo.interestrate.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * For backward compatibility - same as AbstractTermStructureMonteCarloProduct.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractLIBORMonteCarloProduct extends AbstractTermStructureMonteCarloProduct {

	/**
	 * @param currency The currency of this product (may be null for "any currency").
	 */
	public AbstractLIBORMonteCarloProduct(final String currency) {
		super(currency);
	}

	/**
	 *
	 */
	public AbstractLIBORMonteCarloProduct() {
		super(null);
	}

	public abstract RandomVariable getValue(double evaluationTime, LIBORModelMonteCarloSimulationModel model) throws CalculationException;

	@Override
	public RandomVariable getValue(double evaluationTime, TermStructureMonteCarloSimulationModel model) throws CalculationException {
		if(model instanceof LIBORModelMonteCarloSimulationModel) {
			return getValue(evaluationTime, (LIBORModelMonteCarloSimulationModel)model);
		}
		else {
			throw new IllegalArgumentException("This product requires a model implementing LIBORModelMonteCarloSimulationModel. Model is " + model.getClass().getSimpleName());
		}
	}
}
