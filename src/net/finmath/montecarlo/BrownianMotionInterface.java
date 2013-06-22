/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2010
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.ImmutableRandomVariableInterface;
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
 * @version 1.2
 */
public interface BrownianMotionInterface {

	/**
	 * @param timeIndex The time index (corresponding to the this class's time discretization)
	 * @param factor The index of the factor (independent scalar Brownian increment)
	 * @return The factor (component) of the Brownian increments (a random variable)
	 */
	ImmutableRandomVariableInterface getBrownianIncrement(int timeIndex, int factor);


	/**
	 * @return The time discretization used for this set of time-discrete Brownian increments.
	 */
    TimeDiscretizationInterface getTimeDiscretization();

	/**
	 * @return The number of factors.
	 */
    int getNumberOfFactors();

	/**
	 * @return The number of paths.
	 */
    int getNumberOfPaths();
	
	/**
	 * Return a new object implementing BrownianMotionInterface
	 * having the same specifications as this object but a different seed.
	 * 
	 * This method is useful if you like to make Monte-Carlo samplings by changing
	 * the seed.
	 * 
	 * @param seed New value for the seed.
	 * @return New object implementing BrownianMotionInterface.
	 */
    BrownianMotionInterface getCloneWithModifiedSeed(int seed);
}