package net.finmath.montecarlo.interestrate.models.funding;

import net.finmath.stochastic.RandomVariable;

public class DefaultFactors {
	private final RandomVariable survivalProbability;
	private final RandomVariable defaultCompensation;

	public DefaultFactors(RandomVariable survivalProbability, RandomVariable defaultCompensation) {
		this.survivalProbability = survivalProbability;
		this.defaultCompensation = defaultCompensation;
	}

	public RandomVariable getSurvivalProbability() {
		return survivalProbability;
	}

	public RandomVariable getDefaultCompensation() {
		return defaultCompensation;
	}
}