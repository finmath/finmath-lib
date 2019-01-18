/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 13.08.2004
 */
package net.finmath.stochastic;

/**
 * The interface which has to be implemented by a fixed conditional expectation operator,
 *  i.e., E( &middot; | Z ) for a fixed Z.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface ConditionalExpectationEstimator {

	/**
	 * Return the conditional expectation of a given random variable.
	 * The definition of the filtration time is part of the object implementing this interface.
	 *
	 * @param randomVariable Given random variable.
	 * @return The conditional expectation of <code>randomVariable</code>.
	 */
	RandomVariable getConditionalExpectation(RandomVariable randomVariable);
}
