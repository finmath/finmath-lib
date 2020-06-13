/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 05.07.2014
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * Interface description of a time-discrete n-dimensional stochastic process
 * \( X = (X_{1},\ldots,X_{n}) \) provided by independent
 * increments \( \Delta X(t_{i}) = X(t_{i+1})-X(t_{i}) \).
 *
 * Here the dimension <i>n</i> is called factors since this process is used to
 * generate multi-dimensional multi-factor processes and there one might
 * use a different number of factors to generate processes of different
 * dimension.
 *
 * @author Christian Fries
 * @version 1.3
 */
public interface IndependentIncrements {

	/**
	 * Return the increment for a given timeIndex.
	 *
	 * The method returns the random variable vector
	 *	 <i>&Delta; X(t<sub>i</sub>) := X(t<sub>i+1</sub>)-X(t<sub>i</sub>)</i>
	 * for the given time index <i>i</i>.
	 *
	 * @param timeIndex The time index (corresponding to the this class's time discretization)
	 * @return The vector-valued increment (as a vector (array) of random variables).
	 */
	default RandomVariable[] getIncrement(final int timeIndex)
	{
		final RandomVariable[] increment = new RandomVariable[getNumberOfFactors()];
		for(int factorIndex = 0; factorIndex<getNumberOfFactors(); factorIndex++) {
			increment[factorIndex] = getIncrement(timeIndex, factorIndex);
		}
		return increment;
	}

	/**
	 * Return the increment for a given timeIndex and given factor.
	 *
	 * The method returns the random variable
	 *	 <i>&Delta; X<sub>j</sub>(t<sub>i</sub>) := X<sub>j</sub>(t<sub>i+1</sub>)-X(t<sub>i</sub>)</i>
	 * for the given time index <i>i</i> and a given factor (index) <i>j</i>
	 *
	 * @param timeIndex The time index (corresponding to the this class's time discretization)
	 * @param factor The index of the factor (independent scalar increment)
	 * @return The factor (component) of the increments (a random variable)
	 */
	RandomVariable getIncrement(int timeIndex, int factor);


	/**
	 * Returns the time discretization used for this set of time-discrete Brownian increments.
	 *
	 * @return The time discretization used for this set of time-discrete Brownian increments.
	 */
	TimeDiscretization getTimeDiscretization();

	/**
	 * Returns the number of factors.
	 *
	 * @return The number of factors.
	 */
	int getNumberOfFactors();

	/**
	 * Returns the number of paths.
	 *
	 * @return The number of paths.
	 */
	int getNumberOfPaths();

	/**
	 * Returns a random variable which is initialized to a constant,
	 * but has exactly the same number of paths or discretization points as the ones used by this BrownianMotion.
	 *
	 * @param value The constant value to be used for initialized the random variable.
	 * @return A new random variable.
	 */
	RandomVariable getRandomVariableForConstant(double value);

	/**
	 * Return a new object implementing BrownianMotion
	 * having the same specifications as this object but a different seed
	 * for the random number generator.
	 *
	 * This method is useful if you like to make Monte-Carlo samplings by changing
	 * the seed.
	 *
	 * @param seed New value for the seed.
	 * @return New object implementing BrownianMotion.
	 */
	IndependentIncrements getCloneWithModifiedSeed(int seed);


	/**
	 * Return a new object implementing BrownianMotion
	 * having the same specifications as this object but a different
	 * time discretization.
	 *
	 * @param newTimeDiscretization New time discretization
	 * @return New object implementing BrownianMotion.
	 */
	IndependentIncrements getCloneWithModifiedTimeDiscretization(TimeDiscretization newTimeDiscretization);
}
