/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * A factory (helper class) to create random variables.
 *
 * By changing the factory implementation used, you can (more or less globally)
 * change which implementation of random variable is used.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class RandomVariableFactory extends AbstractRandomVariableFactory {

	final boolean isUseDoublePrecisionFloatingPointImplementation;

	public RandomVariableFactory() {
		super();
		this.isUseDoublePrecisionFloatingPointImplementation = true;
	}

	public RandomVariableFactory(boolean isUseDoublePrecisionFloatingPointImplementation) {
		super();
		this.isUseDoublePrecisionFloatingPointImplementation = isUseDoublePrecisionFloatingPointImplementation;
	}

	@Override
	public RandomVariableInterface createRandomVariable(double time, double value) {
		if(isUseDoublePrecisionFloatingPointImplementation) {
			return new RandomVariable(time, value);
		} else {
			return new RandomVariableLowMemory(time, value);
		}
	}

	@Override
	public RandomVariableInterface createRandomVariable(double time, double[] values) {
		if(isUseDoublePrecisionFloatingPointImplementation) {
			return new RandomVariable(time, values);
		} else {
			return new RandomVariableLowMemory(time, values);
		}
	}
}
