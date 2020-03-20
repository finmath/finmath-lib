/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 13.09.2014
 */

package net.finmath.marketdata2.calibration;

import net.finmath.stochastic.RandomVariable;

/**
 * Interface for parameter transformation. A parameter transformation can be used
 * to implement constrains.
 *
 * Example: The solver/optimizer assumes that its parameters are unbounded, i.e.
 * admissible values are searched in the range of {@link java.lang.Double#NEGATIVE_INFINITY} to {@link java.lang.Double#POSITIVE_INFINITY}.
 * If you like to constrain the first parameter to be &gt; 0, then you might use the parameter transformation where
 * getParameter is just Math:exp and getSolverParameter is Math:log.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface ParameterTransformation {

	/**
	 * Return the original parameter for the given (unbounded) solver parameter.
	 *
	 * @param solverParameter The given solver parameter (unbounded).
	 * @return The (possibly bounded) parameter.
	 */
	RandomVariable[] getParameter(RandomVariable[] solverParameter);

	/**
	 * Return the (unbounded) solver parameter for the given original parameter.
	 *
	 * @param parameter The parameter.
	 * @return The corresponding unbounded solver parameter.
	 */
	RandomVariable[] getSolverParameter(RandomVariable[] parameter);
}
