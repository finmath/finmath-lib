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
 * W = (W(1),...,W(n)) where W(i) is a Brownian motion and
 * W(i),W(j) are independent for i not equal j.
 * 
 * Here the dimension n is called factors since this Brownian motion is used to
 * generate multi-dimensional multi-factor Ito processes and there one might
 * use a different number of factors to generate Ito processes of different
 * dimension. 
 * 
 * @author Christian Fries
 * @version 1.0
 */
public interface BrownianMotionInterface {

	/**
	 * @param timeIndex The time index (corresponding to the this class's time discretization)
	 * @param factor The index of the factor (independent scalar Brownian increment)
	 * @return The factor (component) of the Brownian increments (a random variable)
	 */
	public abstract ImmutableRandomVariableInterface getBrownianIncrement(int timeIndex, int factor);


	/**
	 * @return The time discretization used for this set of time-discrete Brownian increments.
	 */
	public abstract TimeDiscretizationInterface getTimeDiscretization();

	/**
	 * @return The number of factors.
	 */
	public abstract int getNumberOfFactors();

	/**
	 * @return The number of paths.
	 */
	public abstract int getNumberOfPaths();
	
	public abstract Object getCloneWithModifiedSeed(int seed);
}