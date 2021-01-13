package net.finmath.montecarlo.automaticdifferentiation.backward;

import java.util.HashMap;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.stochastic.RandomVariable;

/**
 * Basic test for RandomVariableDifferentiableAAD.
 *
 * @author Christian Fries
 */
public class RandomVariableDifferentiableAADTest {

	@Test
	public void testSecondOrderDerivative() {

		/*
		 * Note: This is still experimental, but it shows that second order derivatives are possible by
		 * differentiation of the first order derivative.
		 * Handling and assignment of IDs is subject to change.
		 */
		final RandomVariableFactory randomVariableFactoryParameters = new RandomVariableFromArrayFactory();

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactoryVariable = new RandomVariableDifferentiableAADFactory(
				new RandomVariableDifferentiableAADFactory(
						new RandomVariableFromArrayFactory(), properties), properties);

		final RandomVariable a = randomVariableFactoryParameters.createRandomVariable(5.0);
		final RandomVariable b = randomVariableFactoryParameters.createRandomVariable(1.0);
		final RandomVariableDifferentiable x = (RandomVariableDifferentiable) randomVariableFactoryVariable.createRandomVariable(5.0);
		final RandomVariableDifferentiable y = (RandomVariableDifferentiable) randomVariableFactoryVariable.createRandomVariable(2.0);

		// Simple function: (a x)^2 + (b y)^3
		final RandomVariableDifferentiable result = (RandomVariableDifferentiable)x.mult(a).pow(2).add(y.mult(b).pow(3));

		// Get first derivative
		final Map<Long, RandomVariable> dresult = result.getGradient();
		final RandomVariableDifferentiable dx = (RandomVariableDifferentiable)dresult.get(x.getID());

		// Get second order derivative
		final Map<Long, RandomVariable> ddx = dx.getGradient();
		final RandomVariable ddxx = ddx.get(x.getID()-1);				// The -1 here is subject to change. Currently an issue that we need to access the inner ID.

		final double derivativeAAD = ddxx.getAverage();
		final double derivativeExpected = 2 * a.pow(2).mult(x.pow(0)).doubleValue();

		System.out.println("AAD value     : " + derivativeAAD);
		System.out.println("expeted value : " + derivativeExpected);
		System.out.println();

		Assert.assertEquals(derivativeExpected, derivativeAAD, 1E-15);
	}

	@Test
	public void testOperatorAdd1() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x.add(x);

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		Assert.assertEquals(2.0, dydx.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorAdd2() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x0 = randomVariableFactory.createRandomVariable(6.0);
		final RandomVariable x1 = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x0.add(x1);

		final RandomVariable dydx0 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x0).getID());

		Assert.assertEquals("dydx0", 1.0, dydx0.doubleValue(), 1E-15);

		final RandomVariable dydx1 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x1).getID());

		Assert.assertEquals("dydx1", 1.0, dydx1.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorSub1() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x.sub(x);

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		Assert.assertEquals(0.0, dydx.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorSub2() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x0 = randomVariableFactory.createRandomVariable(6.0);
		final RandomVariable x1 = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x0.sub(x1);

		final RandomVariable dydx0 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x0).getID());

		Assert.assertEquals("dydx0", 1.0, dydx0.doubleValue(), 1E-15);

		final RandomVariable dydx1 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x1).getID());

		Assert.assertEquals("dydx1", -1.0, dydx1.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorMult1() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x.mult(x);

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		Assert.assertEquals(2.0 * x.doubleValue(), dydx.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorMult2() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x0 = randomVariableFactory.createRandomVariable(6.0);
		final RandomVariable x1 = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x0.mult(x1);

		final RandomVariable dydx0 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x0).getID());

		Assert.assertEquals("dydx0", x1.doubleValue(), dydx0.doubleValue(), 1E-15);

		final RandomVariable dydx1 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x1).getID());

		Assert.assertEquals("dydx1", x0.doubleValue(), dydx1.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorDiv1() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x.div(x);

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		Assert.assertEquals(0.0, dydx.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorDiv2() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x0 = randomVariableFactory.createRandomVariable(6.0);
		final RandomVariable x1 = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x0.div(x1);

		final RandomVariable dydx0 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x0).getID());

		Assert.assertEquals("dydx0", x1.invert().doubleValue(), dydx0.doubleValue(), 1E-15);

		final RandomVariable dydx1 = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x1).getID());

		Assert.assertEquals("dydx1", x0.div(x1.squared()).mult(-1).doubleValue(), dydx1.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorExp() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x.exp();

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		Assert.assertEquals(y.doubleValue(), dydx.doubleValue(), 1E-15);
	}

	@Test
	public void testOperatorLog() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(2.0);
		final RandomVariable y = x.log();

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		Assert.assertEquals(x.invert().doubleValue(), dydx.doubleValue(), 1E-15);
	}

	@Test
	public void testExpectation() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(0.0, new double[] { 0.0, 2.0, 1.0, -2.0, -1.0 });
		final RandomVariable a = randomVariableFactory.createRandomVariable(0.0, new double[] { 3.0, 3.0, 3.0, 3.0, 3.0 });

		final RandomVariable y = a.mult(x.expectation());

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		// Finite Difference
		final double epsilon = 1E-8;
		final RandomVariable dydxFD = a.mult(x.add(epsilon).expectation()).sub(a.mult(x.add(-epsilon).expectation())).div(2*epsilon);

		// We do not expect that the derivative agrees point-wise, but the expectation should agree.
		Assert.assertEquals(dydx.expectation().doubleValue(), dydxFD.expectation().doubleValue(), 1E-7);
	}

	@Test
	public void testVariance() {

		final Map<String, Object> properties = new HashMap<>();
		properties.put("isGradientRetainsLeafNodesOnly", false);

		final RandomVariableFactory randomVariableFactory = new RandomVariableDifferentiableAADFactory(
				new RandomVariableFromArrayFactory(), properties);


		final RandomVariable x = randomVariableFactory.createRandomVariable(0.0, new double[] { 0.0, 2.0, 1.0, -2.0, -1.0 });
		final RandomVariable a = randomVariableFactory.createRandomVariable(0.0, new double[] { 3.0, 3.0, 3.0, 3.0, 3.0 });

		final RandomVariable y = a.mult(x.variance());

		final RandomVariable dydx = ((RandomVariableDifferentiable)y).getGradient().get(((RandomVariableDifferentiable)x).getID());

		// Finite Difference
		final double epsilon = 1E-8;
		final RandomVariable dydxFD = a.mult(x.add(epsilon).variance()).sub(a.mult(x.add(-epsilon).variance())).div(2*epsilon);

		// We do not expect that the derivative agrees point-wise, but the expectation should agree.
		Assert.assertEquals(dydx.expectation().doubleValue(), dydxFD.expectation().doubleValue(), 1E-7);
	}
}
