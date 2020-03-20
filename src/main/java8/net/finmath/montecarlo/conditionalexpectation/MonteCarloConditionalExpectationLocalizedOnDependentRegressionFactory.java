package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

/**
 * Provides a localized linear regression with an indicator function as localization weight for a vector of regression basis functions.
 *
 * The regression only considers sample paths where \( - M &lt; y_{i} &lt; M \) where M is a multiple of the standard deviation of y.
 *
 * @author Christian Fries
 */
public class MonteCarloConditionalExpectationLocalizedOnDependentRegressionFactory implements MonteCarloConditionalExpectationRegressionFactory {

	private final double standardDeviations;

	public MonteCarloConditionalExpectationLocalizedOnDependentRegressionFactory(final double standardDeviations) {
		super();
		this.standardDeviations = standardDeviations;
	}

	@Override
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(final RandomVariable[] basisFunctionsEstimator, final RandomVariable[] basisFunctionsPredictor) {
		return new MonteCarloConditionalExpectationRegressionLocalizedOnDependents(basisFunctionsEstimator, basisFunctionsPredictor, standardDeviations);
	}

}
