package net.finmath.montecarlo.automaticdifferentiation;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.forward.RandomVariableDifferentiableADFactory;
import net.finmath.stochastic.RandomVariable;

/**
 * Basic test for RandomVariableDifferentiableAAD.
 *
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class RandomVariableDifferentiableTypePriorityTest {

	/* parameters specify the factories one wants to test against each other */
	@Parameters
	public static Collection<Object[]> data(){
		return Arrays.asList(new Object[][] {
			{ new RandomVariableFromArrayFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */), new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{ new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */), new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{ new RandomVariableFromArrayFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */), new RandomVariableDifferentiableADFactory(new RandomVariableFromArrayFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{ new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */), new RandomVariableDifferentiableADFactory(new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */)) },
		});
	}

	private final RandomVariableFactory randomVariableFactoryValue;
	private final RandomVariableFactory randomVariableFactoryDifferentiable;

	public RandomVariableDifferentiableTypePriorityTest(final RandomVariableFactory randomVariableFactoryValue, final AbstractRandomVariableDifferentiableFactory randomVariableFactoryDifferentiable) {
		this.randomVariableFactoryValue = randomVariableFactoryValue;
		this.randomVariableFactoryDifferentiable = randomVariableFactoryDifferentiable;
	}

	@Test
	public void testTypePriorityAdd() {

		final RandomVariable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		final RandomVariable y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * add
		 */

		final RandomVariable z1 = x.add(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		final RandomVariable z2 = y.add(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		System.out.println();

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}

	@Test
	public void testTypePriorityMult() {

		final RandomVariable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		final RandomVariable y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * mult
		 */

		final RandomVariable z1 = x.mult(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		final RandomVariable z2 = y.mult(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}

	@Test
	public void testTypePriorityCap() {

		final RandomVariable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		final RandomVariable y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * cap
		 */

		final RandomVariable z1 = x.cap(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		final RandomVariable z2 = y.cap(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}

	@Test
	public void testTypePriorityFloor() {

		final RandomVariable x = randomVariableFactoryDifferentiable.createRandomVariable(2.0);
		final RandomVariable y = randomVariableFactoryValue.createRandomVariable(3.0);

		System.out.println("Checking the return type of operators upon commutation:");

		/*
		 * floor
		 */

		final RandomVariable z1 = x.floor(y);
		System.out.println("Value:" + z1.getAverage() + "\t Class" + z1.getClass());
		Assert.assertSame("Return type class", x.getClass(), z1.getClass());

		final RandomVariable z2 = y.floor(x);
		System.out.println("Value:" + z2.getAverage() + "\t Class" + z2.getClass());
		Assert.assertSame("Return type class", x.getClass(), z2.getClass());	// Applying to y we expect the class of x

		Assert.assertEquals("Value upon commutation", z1.getAverage(), z2.getAverage(), 0.0);
	}
}
