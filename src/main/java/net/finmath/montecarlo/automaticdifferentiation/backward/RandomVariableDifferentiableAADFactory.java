/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 21.06.2017
 */

package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.util.Map;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;

/**
 * @author Christian Fries
 *
 */
public class RandomVariableDifferentiableAADFactory extends AbstractRandomVariableDifferentiableFactory {

	/*
	 * barrierDiracWidth defines the width of the discrete approximation of the dirac peak
	 * from differentiation of a jump (barrier). It corresponds to the finite difference shift
	 * when the derivative is calculated via finite differences.
	 * It is a multiplicator to the standard deviation of the random variable.
	 */
	private final double barrierDiracWidth;
	private final boolean isGradientRetainsLeafNodesOnly;

	public RandomVariableDifferentiableAADFactory() {
		this(new RandomVariableFactory());
	}

	/**
	 * @param randomVariableFactoryForNonDifferentiable Random variable factory for the underlying values.
	 */
	public RandomVariableDifferentiableAADFactory(AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable) {
		super(randomVariableFactoryForNonDifferentiable);
		
		barrierDiracWidth = 0.2;
		isGradientRetainsLeafNodesOnly = true;
	}

	public RandomVariableDifferentiableAADFactory(AbstractRandomVariableFactory randomVariableFactoryForNonDifferentiable, Map<String, Object> properties) {
		super(randomVariableFactoryForNonDifferentiable);
		
		barrierDiracWidth = (Double) properties.getOrDefault("barrierDiracWidth", new Double(0.2));
		isGradientRetainsLeafNodesOnly = (Boolean) properties.getOrDefault("isGradientRetainsLeafNodesOnly", new Boolean(true));
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double value) {
		return new RandomVariableDifferentiableAAD(createRandomVariableNonDifferentiable(time, value), this);
	}

	@Override
	public RandomVariableDifferentiableInterface createRandomVariable(double time, double[] values) {
		return new RandomVariableDifferentiableAAD(createRandomVariableNonDifferentiable(time, values), this);
	}

	public double getBarrierDiracWidth() {
		return barrierDiracWidth;
	}

	public boolean isGradientRetainsLeafNodesOnly() {
		return isGradientRetainsLeafNodesOnly;
	}
}
