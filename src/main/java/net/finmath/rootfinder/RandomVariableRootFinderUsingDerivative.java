/**
 * 
 */
package net.finmath.rootfinder;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Stefan Sedlmair
 *
 */
public interface RandomVariableRootFinderUsingDerivative {

	/**
	 * @return Next point for which a value should be set using <code>setValue</code>.
	 */
    RandomVariableInterface getNextPoint();
	
	/**
	 * @param value The value corresponding to the point returned by previous <code>getNextPoint</code> call.
	 * @param derivative The derivative corresponding to the point returned by previous <code>getNextPoint</code> call.
	 */
    void setValueAndDerivative(RandomVariableInterface value, RandomVariableInterface derivative);

	/**
	 * @return Returns the numberOfIterations.
	 */
    int getNumberOfIterations();
	
	/**
	 * @return Returns the accuracy.
	 */
    double getAccuracy();
	
	/**
	 * @return Returns the isDone.
	 */
    boolean isDone();

	/**
	 * @return Best point optained so far
	 */
    RandomVariableInterface getBestPoint();
}

