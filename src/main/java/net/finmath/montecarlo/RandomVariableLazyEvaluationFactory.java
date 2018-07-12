/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariableInterface;

/**
 *
 * @author Christian Fries
 */
public class RandomVariableLazyEvaluationFactory extends AbstractRandomVariableFactory {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5474699190536441150L;

	@Override
	public RandomVariableInterface createRandomVariable(double time, double value) {
		return new RandomVariableLazyEvaluation(time, value);
	}

	@Override
	public RandomVariableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableLazyEvaluation(time, values);
	}
}
