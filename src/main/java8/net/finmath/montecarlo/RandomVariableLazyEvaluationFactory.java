/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariable;

/**
 *
 * @author Christian Fries
 * @version 1.0
 */
public class RandomVariableLazyEvaluationFactory extends AbstractRandomVariableFactory {

	/**
	 *
	 */
	private static final long serialVersionUID = 5474699190536441150L;

	@Override
	public RandomVariable createRandomVariable(final double time, final double value) {
		return new RandomVariableLazyEvaluation(time, value);
	}

	@Override
	public RandomVariable createRandomVariable(final double time, final double[] values) {
		return new RandomVariableLazyEvaluation(time, values);
	}
}
