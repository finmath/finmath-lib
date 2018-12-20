/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class RandomVariableDifferentiableAADStochasticNonOptimizedFactory extends AbstractRandomVariableDifferentiableFactory {

	/**
	 *
	 */
	private static final long serialVersionUID = 2297640879918234200L;

	/**
	 *
	 */
	public RandomVariableDifferentiableAADStochasticNonOptimizedFactory() {
		super();
	}

	/**
	 * @param randomVariableFactoryForNonDifferentiable Random variable facotory for the underlying values.
	 */
	public RandomVariableDifferentiableAADStochasticNonOptimizedFactory(AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super(randomVariableFactoryForNonDifferentiable);
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double value) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(new RandomVariable(time, value));
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableDifferentiableAADStochasticNonOptimized(new RandomVariable(time, values));
	}
}
