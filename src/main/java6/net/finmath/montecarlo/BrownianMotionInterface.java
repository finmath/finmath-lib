/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2010
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Interface description of a time-discrete n-dimensional Brownian motion
 * <i>W = (W<sub>1</sub>,...,W<sub>n</sub>)</i> where <i>W<sub>i</sub></i> is a Brownian motion.
 * 
 * Here the dimension <i>n</i> is called factors since this Brownian motion is used to
 * generate multi-dimensional multi-factor Ito processes and there one might
 * use a different number of factors to generate Ito processes of different
 * dimension. 
 * 
 * @author Christian Fries
 * @version 1.3
 */
public interface BrownianMotionInterface {

	/**
	 * Return the Brownian increment for a given timeIndex.
	 * 
	 * The method returns the random variable
	 *	 <i>&Delta; W<sub>j</sub>(t<sub>i</sub>) := W<sub>j</sub>(t<sub>i+1</sub>)-W(t<sub>i</sub>)</i>
	 * for the given time index <i>i</i> and a given factor (index) <i>j</i>
	 * 
	 * @param timeIndex The time index (corresponding to the this class's time discretization).
	 * @param factor The index of the factor (independent scalar Brownian increment).
	 * @return The factor (component) of the Brownian increments (a random variable).
	 */
	RandomVariableInterface getBrownianIncrement(int timeIndex, int factor);


	/**
	 * Returns the time discretization used for this set of time-discrete Brownian increments.
	 * 
	 * @return The time discretization used for this set of time-discrete Brownian increments.
	 */
	TimeDiscretizationInterface getTimeDiscretization();

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
	 * but has exactly the same number of paths or discretization points as the ones used by this BrownianMotionInterface.
	 *
	 * @param value The constant value to be used for initialized the random variable.
	 * @return A new random variable.
	 */
	RandomVariableInterface getRandomVariableForConstant(double value);

	/**
	 * Return a new object implementing BrownianMotionInterface
	 * having the same specifications as this object but a different seed
	 * for the random number generator.
	 * 
	 * This method is useful if you like to make Monte-Carlo samplings by changing
	 * the seed.
	 * 
	 * @param seed New value for the seed.
	 * @return New object implementing BrownianMotionInterface.
	 */
	BrownianMotionInterface getCloneWithModifiedSeed(int seed);


	/**
	 * Return a new object implementing BrownianMotionInterface
	 * having the same specifications as this object but a different
	 * time discretization.
	 * 
	 * @param newTimeDiscretization New time discretization
	 * @return New object implementing BrownianMotionInterface.
	 */
	BrownianMotionInterface getCloneWithModifiedTimeDiscretization(TimeDiscretizationInterface newTimeDiscretization);
}