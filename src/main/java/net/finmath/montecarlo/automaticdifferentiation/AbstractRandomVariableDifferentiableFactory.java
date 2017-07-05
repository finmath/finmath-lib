/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 02.07.2017
 */

package net.finmath.montecarlo.automaticdifferentiation;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 */
public abstract class AbstractRandomVariableDifferentiableFactory extends AbstractRandomVariableFactory {

	private final AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable;

	public AbstractRandomVariableDifferentiableFactory() {
		super();
		this.randomVariableFactoryForNonDifferentiable = new RandomVariableFactory();
	}

	public AbstractRandomVariableDifferentiableFactory(AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super();
		this.randomVariableFactoryForNonDifferentiable = randomVariableFactoryForNonDifferentiable;
	}
	
	public RandomVariableDifferentiableInterface createRandomVariable(double value) {
		return createRandomVariable(0.0, value);
	}

	public abstract RandomVariableDifferentiableInterface createRandomVariable(double time, double value);

	public abstract RandomVariableDifferentiableInterface createRandomVariable(double time, double[] values);

	public RandomVariableInterface createRandomVariableNonDifferentiable(double time, double value) {
		return randomVariableFactoryForNonDifferentiable.createRandomVariable(time, value);
	}

	public RandomVariableInterface createRandomVariableNonDifferentiable(double time, double[] values) {
		return randomVariableFactoryForNonDifferentiable.createRandomVariable(time, values);
	}
}
