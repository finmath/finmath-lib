/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo;

import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 */
public class RandomVariableFloatFactory extends AbstractRandomVariableFactory {

	private static final long serialVersionUID = 3368581641610610123L;

	public RandomVariableFloatFactory() {
		super();
	}

	@Override
	public RandomVariable createRandomVariable(final double time, final double value) {
		return new RandomVariableFromFloatArray(time, value);
	}

	@Override
	public RandomVariable createRandomVariable(final double time, final double[] values) {
		return new RandomVariableFromFloatArray(time, values);
	}
}
