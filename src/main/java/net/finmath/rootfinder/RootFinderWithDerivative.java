/*
 * Created on 30.05.2004
 *
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 */
package net.finmath.rootfinder;

/**
 * This is the interface for a one dimensional root finder
 * implemented as an question-and-answer algorithm.
 *
 * @author Christian Fries
 * @date 2008-04-06
 * @version 1.0
 */
public interface RootFinderWithDerivative {

	/**
	 * @return Next point for which a value should be set using <code>setValue</code>.
	 */
	double getNextPoint();

	/**
	 * @param value The value corresponding to the point returned by previous <code>getNextPoint</code> call.
	 * @param derivative The derivative corresponding to the point returned by previous <code>getNextPoint</code> call.
	 */
	void setValueAndDerivative(double value, double derivative);

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
	double getBestPoint();
}
