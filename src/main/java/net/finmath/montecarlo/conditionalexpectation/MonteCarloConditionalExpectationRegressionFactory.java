package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

/**
 * Interface implemented by classes providing a <code>ConditionalExpectationEstimator</code> for conditional expectation estimation.
 *
 * @author Christian Fries
 */
public interface MonteCarloConditionalExpectationRegressionFactory {

	/**
	 * Creates an object implementing a <code>ConditionalExpectationEstimator</code> for conditional expectation estimation.
	 *
	 * @param basisFunctionsEstimator A vector of random variables to be used as basis functions for estimation.
	 * @param basisFunctionsPredictor A vector of random variables to be used as basis functions for prediction.
	 * @return An object implementing a <code>ConditionalExpectationEstimator</code>.
	 */
	ConditionalExpectationEstimator getConditionalExpectationEstimator(RandomVariable[] basisFunctionsEstimator, RandomVariable[] basisFunctionsPredictor);

}
