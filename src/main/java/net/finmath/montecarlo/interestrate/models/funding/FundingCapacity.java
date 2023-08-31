package net.finmath.montecarlo.interestrate.models.funding;

import net.finmath.stochastic.RandomVariable;

public interface FundingCapacity {

	/**
	 * Apply a new funding requirement to this funding capacity
	 * and return the associated <code>DefaultFactors</code>.
	 * 
	 * @param time The time at which the funding is required.
	 * @param fundingRequirement The required funding.
	 * @return A <code>DefaultFactors</code> that reflects the amount that has to be contracted to secure the funding.
	 */
	DefaultFactors getDefaultFactors(double time, RandomVariable fundingRequirement);

}