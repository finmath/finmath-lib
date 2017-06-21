/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public class RandomVariableDifferentiableFactory extends AbstractRandomVariableFactory {

	/**
	 * 
	 */
	public RandomVariableDifferentiableFactory() {
	}

	@Override
	public RandomVariableInterface createRandomVariable(double time, double value) {
		return new RandomVariableDifferentiableAAD(new RandomVariable(time, value));
	}

	@Override
	public RandomVariableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableDifferentiableAAD(new RandomVariable(time, values));
	}
}
