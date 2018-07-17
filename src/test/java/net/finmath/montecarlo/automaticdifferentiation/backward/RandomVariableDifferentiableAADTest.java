package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;

public class RandomVariableDifferentiableAADTest {

	@Test
	public void testSecondOrderDerivative() {

		/*
		 * Note: This is still experimental, but it shows that second order derivatives are possible by
		 * differentiation of the first order derivative.
		 * Handling and assignment of IDs is subject to change.
		 */
		AbstractRandomVariableFactory randomVariableFactoryParameters = new RandomVariableFactory();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", new Boolean(false));

		AbstractRandomVariableFactory randomVariableFactoryVariable = new RandomVariableDifferentiableAADFactory(
				new RandomVariableDifferentiableAADFactory(
						new RandomVariableFactory(), properties), properties);

		RandomVariableInterface a = randomVariableFactoryParameters.createRandomVariable(5.0);
		RandomVariableInterface b = randomVariableFactoryParameters.createRandomVariable(1.0);
		RandomVariableDifferentiableInterface x = (RandomVariableDifferentiableInterface) randomVariableFactoryVariable.createRandomVariable(5.0);
		RandomVariableDifferentiableInterface y = (RandomVariableDifferentiableInterface) randomVariableFactoryVariable.createRandomVariable(2.0);

		// Simple function: (a x)^2 + (b y)^3
		RandomVariableDifferentiableInterface result = (RandomVariableDifferentiableInterface)x.mult(a).pow(2).add(y.mult(b).pow(3));

		// Get first derivative
		Map<Long, RandomVariableInterface> dresult = result.getGradient();
		RandomVariableDifferentiableInterface dx = (RandomVariableDifferentiableInterface)dresult.get(x.getID());

		// Get second order derivative
		Map<Long, RandomVariableInterface> ddx = dx.getGradient();
		RandomVariableInterface ddxx = ddx.get(x.getID()-1);				// The -1 here is subject to change. Currently an issue that we need to access the inner ID.

		double derivativeAAD = ddxx.getAverage();
		double derivativeExpected = 2 * a.pow(2).mult(x.pow(0)).doubleValue();

		System.out.println("AAD value     : " + derivativeAAD);
		System.out.println("expeted value : " + derivativeExpected);

		Assert.assertEquals(derivativeExpected, derivativeAAD, 1E-15);
	}
}
