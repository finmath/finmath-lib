package net.finmath.rootfinder;

import net.finmath.stochastic.RandomVariable;

/**
 * Interface for root finders for stochastic maps \( y = f(x) \) where \( x,y \) are random variables, the map is pathwise and the pathwise derivative
 * is known.
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.0
 */
public interface StochasticRootFinderUsingDerivative {

	/**
	 * @return Next point for which a value should be set using <code>setValue</code>.
	 */
	RandomVariable getNextPoint();

	/**
	 * @param value The value corresponding to the point returned by previous <code>getNextPoint</code> call.
	 * @param derivative The derivative corresponding to the point returned by previous <code>getNextPoint</code> call.
	 */
	void setValueAndDerivative(RandomVariable value, RandomVariable derivative);

	/**
	 * @return Returns the numberOfIterations.
	 */
	int getNumberOfIterations();

	/**
	 * @return Best point obtained so far
	 */
	RandomVariable getBestPoint();

	/**
	 * @return Returns the accuracy.
	 */
	double getAccuracy();

	/**
	 * @return Returns true if further improvement is not possible.
	 */
	boolean isDone();
}

