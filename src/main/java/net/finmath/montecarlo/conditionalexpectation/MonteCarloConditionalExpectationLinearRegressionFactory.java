package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

/**
 * Provides a linear regression for a vector of regression basis functions.
 *
 * @author Christian Fries
 */
public class MonteCarloConditionalExpectationLinearRegressionFactory implements MonteCarloConditionalExpectationRegressionFactory {

	@Override
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(final RandomVariable[] basisFunctionsEstimator, final RandomVariable[] basisFunctionsPredictor) {
		return new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsPredictor);
	}

}
