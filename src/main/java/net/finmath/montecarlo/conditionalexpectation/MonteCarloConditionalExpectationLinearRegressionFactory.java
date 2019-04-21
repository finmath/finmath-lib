package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

public class MonteCarloConditionalExpectationLinearRegressionFactory implements MonteCarloConditionalExpectationRegressionFactory {

	@Override
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(RandomVariable[] basisFunctionsEstimator, RandomVariable[] basisFunctionsPredictor) {
		return new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsPredictor);
	}

}
