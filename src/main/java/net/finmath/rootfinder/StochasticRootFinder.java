package net.finmath.rootfinder;

import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @version 1.0
 */
public interface StochasticRootFinder {

	/**
	 * @return Next point for which a value should be set using <code>setValue</code>.
	 */
	RandomVariable getNextPoint();

	/**
	 * @param value Value corresponding to point returned
	 * by previous <code>getNextPoint</code> call.
	 */
	void setValue(RandomVariable value);

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
