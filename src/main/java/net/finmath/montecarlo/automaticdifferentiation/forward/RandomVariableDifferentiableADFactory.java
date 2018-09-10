/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.forward;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class RandomVariableDifferentiableADFactory extends AbstractRandomVariableDifferentiableFactory {

	public RandomVariableDifferentiableADFactory() {
		super(new RandomVariableFactory());
	}

	/**
	 * @param randomVariableFactoryForNonDifferentiable Random variable facotory for the underlying values.
	 */
	public RandomVariableDifferentiableADFactory(AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super(randomVariableFactoryForNonDifferentiable);
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double value) {
		return new RandomVariableDifferentiableAD(createRandomVariableNonDifferentiable(time, value));
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableDifferentiableAD(createRandomVariableNonDifferentiable(time, values));
	}
}
