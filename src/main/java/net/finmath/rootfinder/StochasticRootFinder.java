package net.finmath.rootfinder;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 * @author Stefan Sedlmair
 */
public interface StochasticRootFinder {

	/**
	 * @return Next point for which a value should be set using <code>setValue</code>.
	 */
	RandomVariableInterface getNextPoint();

	/**
	 * @param value Value corresponding to point returned
	 * by previous <code>getNextPoint</code> call.
	 */
	void setValue(RandomVariableInterface value);

	/**
	 * @return Returns the numberOfIterations.
	 */
	int getNumberOfIterations();

	/**
	 * @return Best point obtained so far
	 */
	RandomVariableInterface getBestPoint();

	/**
	 * @return Returns the accuracy.
	 */
	double getAccuracy();

	/**
	 * @return Returns true if further improvement is not possible.
	 */
	boolean isDone();
}
