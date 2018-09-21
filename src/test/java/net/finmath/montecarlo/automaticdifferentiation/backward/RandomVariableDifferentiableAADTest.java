package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.AbstractRandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Basic test for RandomVariableDifferentiableAAD.
 *
 * @author Christian Fries
 */
public class RandomVariableDifferentiableAADTest {

	@Test
	public void testArithmeticSubSelf() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", true);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				randomVariableFactoryValue, properties);

		RandomVariableDifferentiableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.sub(x):");

		/*
		 * x.sub(x)
		 */

		RandomVariableInterface z = x.sub(x);
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariableInterface> gradient = ((RandomVariableDifferentiableInterface)z).getGradient();
		RandomVariableInterface dx = gradient.get(x.getID());

		System.out.println("Derivative:" + z.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x-x)", 0.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticDivSelf() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", true);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				randomVariableFactoryValue, properties);

		RandomVariableDifferentiableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.div(x):");

		/*
		 * x.div(x)
		 */

		RandomVariableInterface z = x.div(x);
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariableInterface> gradient = ((RandomVariableDifferentiableInterface)z).getGradient();
		RandomVariableInterface dx = gradient.get(x.getID());

		System.out.println("Derivative:" + z.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 0.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticExpLog() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", true);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				randomVariableFactoryValue, properties);

		RandomVariableDifferentiableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.log().exp():");

		/*
		 * x.log.exp()
		 */

		RandomVariableInterface z = x.log().exp();
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariableInterface> gradient = ((RandomVariableDifferentiableInterface)z).getGradient();
		RandomVariableInterface dx = gradient.get(x.getID());

		System.out.println("Derivative:" + dx.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 1.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticSqrtSquared() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", true);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				randomVariableFactoryValue, properties);

		RandomVariableDifferentiableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.sqrt().squared():");

		/*
		 * x.sqrt.squared()
		 */

		RandomVariableInterface z = x.sqrt().squared();
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariableInterface> gradient = ((RandomVariableDifferentiableInterface)z).getGradient();
		RandomVariableInterface dx = gradient.get(x.getID());

		System.out.println("Derivative:" + dx.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 1.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticSqrtMultSqrt() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", true);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				randomVariableFactoryValue, properties);

		RandomVariableDifferentiableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.sqrt().mult(x.sqrt()):");

		/*
		 * x.sqrt.mult(x.sqrt())
		 */

		RandomVariableInterface z = x.sqrt().mult(x.sqrt());
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariableInterface> gradient = ((RandomVariableDifferentiableInterface)z).getGradient();
		RandomVariableInterface dx = gradient.get(x.getID());

		System.out.println("Derivative:" + dx.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 1.0, dx.getAverage(), 0.0);
	}	

	@Test
	public void testTypePriorityAdd() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFactory(), properties);

		RandomVariableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		RandomVariableInterface y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * add
		 */

		RandomVariableInterface z1 = x.add(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		RandomVariableInterface z2 = y.add(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		System.out.println();

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}

	@Test
	public void testTypePriorityMult() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFactory(), properties);

		RandomVariableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		RandomVariableInterface y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * mult
		 */

		RandomVariableInterface z1 = x.mult(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		RandomVariableInterface z2 = y.mult(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}

	@Test
	public void testTypePriorityCap() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFactory(), properties);

		RandomVariableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		RandomVariableInterface y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * cap
		 */

		RandomVariableInterface z1 = x.cap(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		RandomVariableInterface z2 = y.cap(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}

	@Test
	public void testTypePriorityFloor() {
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		AbstractRandomVariableFactory randomVariableFactoryValue = new RandomVariableFactory();

		AbstractRandomVariableFactory randomVariableFactoryDifferentiable = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFactory(), properties);

		RandomVariableInterface x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		RandomVariableInterface y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * floor
		 */

		RandomVariableInterface z1 = x.floor(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		RandomVariableInterface z2 = y.floor(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}

	@Test
	public void testSecondOrderDerivative() {

		/*
		 * Note: This is still experimental, but it shows that second order derivatives are possible by
		 * differentiation of the first order derivative.
		 * Handling and assignment of IDs is subject to change.
		 */
		AbstractRandomVariableFactory randomVariableFactoryParameters = new RandomVariableFactory();

		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

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
		System.out.println();

		Assert.assertEquals(derivativeExpected, derivativeAAD, 1E-15);
	}
}
