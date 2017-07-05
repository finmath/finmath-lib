/**
 * 
 */
package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Stefan Sedlmair
 *
 */
public class RandomVariableAADv2Factory extends AbstractRandomVariableDifferentiableFactory {

	/**
	 * @param randomVariableFactoryForNonDifferentiable
	 */
	public RandomVariableAADv2Factory(AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super(randomVariableFactoryForNonDifferentiable);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.AbstractRandomVariableFactory#createRandomVariable(double, double)
	 */
	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double value) {
		return new RandomVariableAADv2(time, value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.AbstractRandomVariableFactory#createRandomVariable(double, double[])
	 */
	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableAADv2(time, values);
	}

}
