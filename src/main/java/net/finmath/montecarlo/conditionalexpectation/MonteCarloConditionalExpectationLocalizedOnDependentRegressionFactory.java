package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

public class MonteCarloConditionalExpectationLocalizedOnDependentRegressionFactory implements MonteCarloConditionalExpectationRegressionFactory {

	private final double standardDeviations;

	public MonteCarloConditionalExpectationLocalizedOnDependentRegressionFactory(double standardDeviations) {
		super();
		this.standardDeviations = standardDeviations;
	}

	@Override
	public ConditionalExpectationEstimator getConditionalExpectationEstimator(RandomVariable[] basisFunctionsEstimator, RandomVariable[] basisFunctionsPredictor) {
		return new MonteCarloConditionalExpectationRegressionLocalizedOnDependents(basisFunctionsEstimator, basisFunctionsPredictor, standardDeviations);
	}

}
