/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class RandomVariableDifferentiableAADPathwiseFactory extends AbstractRandomVariableDifferentiableFactory {


	/**
	 *
	 */
	private static final long serialVersionUID = 178082076449157040L;

	/**
	 *
	 */
	public RandomVariableDifferentiableAADPathwiseFactory() {
		super();
	}

	/**
	 * @param randomVariableFactoryForNonDifferentiable Random variable facotory for the underlying values.
	 */
	public RandomVariableDifferentiableAADPathwiseFactory(final RandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super(randomVariableFactoryForNonDifferentiable);
	}

	@Override
	public RandomVariableDifferentiable createRandomVariable(final double time, final double value) {
		return new RandomVariableDifferentiableAADPathwise(new RandomVariableFromDoubleArray(time, value));
	}

	@Override
	public RandomVariableDifferentiable createRandomVariable(final double time, final double[] values) {
		return new RandomVariableDifferentiableAADPathwise(new RandomVariableFromDoubleArray(time, values));
	}
}
