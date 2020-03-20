/*
 * Created on 30.05.2004
 *
 * (c) Copyright Christian P. Fries, Germany.
 * Contact: email@christian-fries.de.
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
public interface RootFinder {

	/**
	 * @return Next point for which a value should be set
	 * using <code>setValue</code>.
	 */
	double getNextPoint();

	/**
	 * @param value Value corresponding to point returned
	 * by previous <code>getNextPoint</code> call.
	 */
	void setValue(double value);

	/**
	 * @return Returns the numberOfIterations.
	 */
	int getNumberOfIterations();

	/**
	 * @return Best point obtained so far
	 */
	double getBestPoint();

	/**
	 * @return Returns the accuracy.
	 */
	double getAccuracy();

	/**
	 * @return Returns true if further improvement is not possible.
	 */
	boolean isDone();

}
