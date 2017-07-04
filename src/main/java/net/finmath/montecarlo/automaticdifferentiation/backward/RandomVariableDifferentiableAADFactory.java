/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public class RandomVariableDifferentiableAADFactory extends AbstractRandomVariableFactory {

	/**
	 * 
	 */
	public RandomVariableDifferentiableAADFactory() {
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double value) {
		return new RandomVariableDifferentiableAAD(new RandomVariable(0.0, value));
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double value) {
		return new RandomVariableDifferentiableAAD(new RandomVariable(time, value));
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableDifferentiableAAD(new RandomVariable(time, values));
	}
}
