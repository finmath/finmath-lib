/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 *
 */
public class RandomVariableDifferentiableAADStochasticNonOptimizedFactory extends AbstractRandomVariableFactory {

	/**
	 * 
	 */
	public RandomVariableDifferentiableAADStochasticNonOptimizedFactory() {
	}

	@Override
	public RandomVariableInterface createRandomVariable(double time, double value) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(new RandomVariable(time, value));
	}

	@Override
	public RandomVariableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(new RandomVariable(time, values));
	}
}
