/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.forward;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class RandomVariableDifferentiableADFactory extends AbstractRandomVariableDifferentiableFactory {

	/**
	 *
	 */
	private static final long serialVersionUID = 252965311623985432L;

	public RandomVariableDifferentiableADFactory() {
		super(new RandomVariableFromArrayFactory());
	}

	/**
	 * @param randomVariableFactoryForNonDifferentiable Random variable facotory for the underlying values.
	 */
	public RandomVariableDifferentiableADFactory(final RandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super(randomVariableFactoryForNonDifferentiable);
	}

	@Override
	public RandomVariableDifferentiable createRandomVariable(final double time, final double value) {
		return new RandomVariableDifferentiableAD(createRandomVariableNonDifferentiable(time, value));
	}

	@Override
	public RandomVariableDifferentiable createRandomVariable(final double time, final double[] values) {
		return new RandomVariableDifferentiableAD(createRandomVariableNonDifferentiable(time, values));
	}

	@Override
	public String toString() {
		return "RandomVariableDifferentiableADFactory [toString()=" + super.toString() + "]";
	}
}
