package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

public interface MonteCarloConditionalExpectationRegressionFactory {
	
	/**
	 * Creates an object implementing a <code>ConditionalExpectationEstimator</code> for conditional expectation estimation.
	 *
	 * @param basisFunctionsEstimator A vector of random variables to be used as basis functions for estimation.
	 * @param basisFunctionsPredictor A vector of random variables to be used as basis functions for prediction.
	 */
	ConditionalExpectationEstimator getConditionalExpectationEstimator(RandomVariable[] basisFunctionsEstimator, RandomVariable[] basisFunctionsPredictor);

}
