/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.08.2018
 */
package net.finmath.montecarlo.conditionalexpectation;

import java.util.List;
import java.util.function.Function;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * An implementation of an RegressionBasisFunctionsProvider using a list of AbstractMonteCarloProduct-s.

 * The getBasisFunctions method will perform a check if the products getValue method returns an
 * \( \mathcal{F}_{t} \)-measurable random variable if called with evaluationTime being \( t \).
 * If this test fails an IllegalArgumentException exception is thrown.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class RegressionBasisFunctionsFromProducts implements RegressionBasisFunctionsProvider {

	private final List<AbstractMonteCarloProduct> products;

	public RegressionBasisFunctionsFromProducts(final List<AbstractMonteCarloProduct> products) {
		super();
		this.products = products;
	}

	@Override
	public RandomVariable[] getBasisFunctions(final double evaluationTime, final MonteCarloSimulationModel model) {

		final Function<AbstractMonteCarloProduct, RandomVariable> valuation = new Function<AbstractMonteCarloProduct, RandomVariable>() {
			@Override
			public RandomVariable apply(final AbstractMonteCarloProduct p) {
				RandomVariable value = null;
				try {
					value = p.getValue(evaluationTime, model);
				} catch (final CalculationException e) {
					throw new IllegalArgumentException("Product " + p + " cannot be valued by model " + model + " at time " + evaluationTime, e);
				}

				if(value.getFiltrationTime() > evaluationTime) {
					throw new IllegalArgumentException(
							"Product " + p + " valued by model " + model + " cannot be used as basis function at time " + evaluationTime + ". "
									+ "Filtration time is " + value.getFiltrationTime());
				}

				return value;
			}
		};

		return products.stream().map(valuation).toArray(RandomVariable[]::new);
	}
}
