package net.finmath.montecarlo.automaticdifferentiation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.forward.RandomVariableDifferentiableADFactory;
import net.finmath.stochastic.RandomVariable;

/**
 * Basic test for RandomVariableDifferentiableAAD.
 *
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class RandomVariableDifferentiableArithmeticTest {

	/* parameters specify the factories one wants to test against each other */
	@Parameters
	public static Collection<Object[]> data(){
		return Arrays.asList(new Object[][] {
			{ new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{ new RandomVariableDifferentiableAADFactory(new RandomVariableFactory(false /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{ new RandomVariableDifferentiableADFactory(new RandomVariableFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{ new RandomVariableDifferentiableADFactory(new RandomVariableFactory(false /* isUseDoublePrecisionFloatingPointImplementation */)) },
		});
	}

	private final AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable;

	public RandomVariableDifferentiableArithmeticTest(AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable) {
		this.randomVariableFactoryDifferentiable = randomVariableFactoryDifferentiable;
	}

	@Test
	public void testArithmeticSubSelf() {

		RandomVariableDifferentiable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.sub(x):");

		/*
		 * x.sub(x)
		 */

		RandomVariable z = x.sub(x);
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)z).getGradient();
		RandomVariable dx = gradient.get(x.getID());

		System.out.println("Derivative:" + z.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x-x)", 0.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticDivSelf() {

		RandomVariableDifferentiable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.div(x):");

		/*
		 * x.div(x)
		 */

		RandomVariable z = x.div(x);
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)z).getGradient();
		RandomVariable dx = gradient.get(x.getID());

		System.out.println("Derivative:" + z.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 0.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticExpLog() {

		RandomVariableDifferentiable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.log().exp():");

		/*
		 * x.log.exp()
		 */

		RandomVariable z = x.log().exp();
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)z).getGradient();
		RandomVariable dx = gradient.get(x.getID());

		System.out.println("Derivative:" + dx.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 1.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticSqrtSquared() {

		RandomVariableDifferentiable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.sqrt().squared():");

		/*
		 * x.sqrt.squared()
		 */

		RandomVariable z = x.sqrt().squared();
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)z).getGradient();
		RandomVariable dx = gradient.get(x.getID());

		System.out.println("Derivative:" + dx.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 1.0, dx.getAverage(), 0.0);
	}

	@Test
	public void testArithmeticSqrtMultSqrt() {

		RandomVariableDifferentiable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);

		System.out.println("Checking x.sqrt().mult(x.sqrt()):");

		/*
		 * x.sqrt.mult(x.sqrt())
		 */

		RandomVariable z = x.sqrt().mult(x.sqrt());
		System.out.println("Value:" + z.getAverage());

		Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)z).getGradient();
		RandomVariable dx = gradient.get(x.getID());

		System.out.println("Derivative:" + dx.getAverage());

		System.out.println();

		Assert.assertEquals("Derivative d/dx (x/x)", 1.0, dx.getAverage(), 0.0);
	}
}
