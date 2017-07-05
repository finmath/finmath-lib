/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;

/**
 * @author Christian Fries
 *
 */
public class RandomVariableDifferentiableAADFactory extends AbstractRandomVariableDifferentiableFactory {

	public RandomVariableDifferentiableAADFactory() {
		super(new RandomVariableFactory());
	}

	/**
	 * @param randomVariableFactoryForNonDifferentiable
	 */
	public RandomVariableDifferentiableAADFactory(AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super(randomVariableFactoryForNonDifferentiable);
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double value) {
		return new RandomVariableDifferentiableAAD(createRandomVariableNonDifferentiable(time, value));
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableDifferentiableAAD(createRandomVariableNonDifferentiable(time, values));
	}
}
