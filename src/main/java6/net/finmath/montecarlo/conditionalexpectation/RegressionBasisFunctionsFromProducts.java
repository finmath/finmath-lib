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
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

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

	public RegressionBasisFunctionsFromProducts(List<AbstractMonteCarloProduct> products) {
		super();
		this.products = products;
	}

	@Override
	public RandomVariableInterface[] getBasisFunctions(final double evaluationTime, final MonteCarloSimulationInterface model) {

		Function<AbstractMonteCarloProduct, RandomVariableInterface> valuation = new Function<AbstractMonteCarloProduct, RandomVariableInterface>() {
			@Override
			public RandomVariableInterface apply(AbstractMonteCarloProduct p) {
				RandomVariableInterface value = null;
				try {
					value = p.getValue(evaluationTime, model);
				} catch (CalculationException e) {
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

		RandomVariableInterface[] basisFunctions = new RandomVariableInterface[products.size()];
		for(int i=0; i<products.size(); i++) {
			basisFunctions[i] = valuation.apply(products.get(i));
		}
		return basisFunctions;
	}
}
