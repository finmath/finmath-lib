/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 13.09.2014
 */
package net.finmath.optimizer.parameter;

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
public interface ParameterTransfrom {

	/**
	 * Transforms a parameter vector x with \( x_{i} \) unbounded
	 * to a model parameter vector y that lies in the domain of
	 * admissible parameters.
	 * 
	 * @param optimizerParameters The given (e.g. unbounded) optimizer parameters.
	 * @return The (e.g. constrained) model parameters.
	 */
	RandomVariable[] getModelParametersFrom(RandomVariable[] optimizerParameters);

	/**
	 * Transforms a model parameter vector y to an
	 * unbounded optimizer parameter x, such that
	 * <code>y</code> equals <code>getModelParametersFrom(x)</code>.
	 * 
	 * @param modelParameters The given (e.g. constrained) model parameters.
	 * @return The (e.g. unbounded) optimizer parameters.
	 */
	RandomVariable[] getOptimizerParametersFrom(RandomVariable[] modelParameters);
}
