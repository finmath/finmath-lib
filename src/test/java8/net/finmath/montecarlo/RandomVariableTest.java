/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo;

import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.forward.RandomVariableDifferentiableADFactory;
import net.finmath.stochastic.RandomVariable;

/**
 * Test cases for the class net.finmath.montecarlo.RandomVariableFromDoubleArray.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.RandomVariableFromDoubleArray
 */
@RunWith(Parameterized.class)
public class RandomVariableTest {

	private final RandomVariableFactory randomVariableFactory;

	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ new RandomVariableFromArrayFactory(true /* isUseDoublePrecisionFloatingPointImplementation */) },
			{ new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */) },
			{ new RandomVariableLazyEvaluationFactory() },
			{ new RandomVariableDifferentiableAADFactory() },
			{ new RandomVariableDifferentiableADFactory() },
		});
	}

	public RandomVariableTest(final RandomVariableFactory randomVariableFactory) {
		super();
		this.randomVariableFactory = randomVariableFactory;
	}


	@Test
	public void testRandomVariableDeterministc() {

		// Create a random variable with a constant
		RandomVariable randomVariable = randomVariableFactory.createRandomVariable(2.0);

		// Perform some calculations
		randomVariable = randomVariable.mult(2.0);
		randomVariable = randomVariable.add(1.0);
		randomVariable = randomVariable.squared();
		randomVariable = randomVariable.sub(4.0);
		randomVariable = randomVariable.div(7.0);

		// The random variable has average value 3.0 (it is constant 3.0)
		Assert.assertEquals(3.0, randomVariable.getAverage(), 0.0);

		// Since the random variable is deterministic, it has zero variance
		Assert.assertEquals(0.0, randomVariable.getVariance(), 0.0);
	}

	@Test
	public void testRandomVariableStochastic() {

		// Create a stochastic random variable
		RandomVariable randomVariable2 = randomVariableFactory.createRandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		// Perform some calculations
		randomVariable2 = randomVariable2.add(4.0);
		randomVariable2 = randomVariable2.div(2.0);

		// The random variable has average value 2.0
		Assert.assertEquals(2.0, randomVariable2.getAverage(), 0.0);

		// The random variable has variance value 2.0 = (4 + 1 + 0 + 1 + 4) / 5
		Assert.assertEquals(2.0, randomVariable2.getVariance(), 1E-12);

		// Multiply two random variables, this will expand the receiver to a stochastic one
		RandomVariable randomVariable = new RandomVariableFromDoubleArray(3.0);
		randomVariable = randomVariable.mult(randomVariable2);

		// The random variable has average value 6.0
		Assert.assertEquals(6.0, randomVariable.getAverage(), 0.0);

		// The random variable has variance value 2 * 9
		Assert.assertEquals(randomVariable.getVariance(), 2.0 * 9.0, 0.0);
	}

	@Test
	public void testRandomVariableArithmeticSqrtPow() {

		// Create a stochastic random variable
		final RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		final RandomVariable check = randomVariable.sqrt().sub(randomVariable.pow(0.5));

		// The random variable is identical 0.0
		Assert.assertEquals(0.0, check.getAverage(), 0.0);
		Assert.assertEquals(0.0, check.getVariance(), 0.0);
	}

	@Test
	public void testRandomVariableArithmeticSquaredPow() {

		// Create a stochastic random variable
		final RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		final RandomVariable check = randomVariable.squared().sub(randomVariable.pow(2.0));

		// The random variable is identical 0.0
		Assert.assertEquals(0.0, check.getAverage(), 0.0);
		Assert.assertEquals(0.0, check.getVariance(), 0.0);
	}

	@Test
	public void testRandomVariableStandardDeviation() {

		// Create a stochastic random variable
		final RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		final double check = randomVariable.getStandardDeviation() - Math.sqrt(randomVariable.getVariance());
		Assert.assertEquals(0.0, check, 0.0);
	}

	/**
	 * Testing quantiles of normal distribution.
	 * Based on feedback provided by Alessandro Gnoatto and a student of him.
	 */
	@Test
	public void testGetQuantile() {

		final int seed = 3141;
		final int numberOfSamplePoints = 10000000;

		final MersenneTwister mersenneTwister = new MersenneTwister(seed);
		final double[] samples = new double[numberOfSamplePoints];
		for(int i = 0; i< numberOfSamplePoints; i++) {
			final double randomNumber = mersenneTwister.nextDouble();
			samples[i] = net.finmath.functions.NormalDistribution.inverseCumulativeDistribution(randomNumber);
		}

		final RandomVariable normalDistributedRandomVariable = randomVariableFactory.createRandomVariable(0.0,samples);

		final double q00 = normalDistributedRandomVariable.getQuantile(0.0);
		Assert.assertEquals(normalDistributedRandomVariable.getMin(), q00, 1E-12);

		final double q05 = normalDistributedRandomVariable.getQuantile(0.05);
		Assert.assertEquals(-1.645, q05, 1E-3);

		final double q50 = normalDistributedRandomVariable.getQuantile(0.5);
		Assert.assertEquals(0, q50, 2E-4);

		final double q95 = normalDistributedRandomVariable.getQuantile(0.95);
		Assert.assertEquals(1.645, q95, 1E-3);

		final double q99 = normalDistributedRandomVariable.getQuantile(0.99);
		Assert.assertEquals(2.33, q99, 1E-2);
	}

	@Test
	public void testAdd() {

		// Create a stochastic random variable
		final RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0,  2.0, 4.0} );

		final RandomVariable randomVariable2 = randomVariableFactory.createRandomVariable(0.0,
				new double[] { 4.0,  2.0, 0.0, -2.0, -4.0} );

		final RandomVariable valueAdd = randomVariable.add(randomVariable2);

		// The random variable average
		Assert.assertEquals(valueAdd.getAverage(), 0.0, 1E-15);

		// The random variable has variance value 0
		Assert.assertEquals(valueAdd.getVariance(), 0.0, 1E-15);
	}

	@Test
	public void testCap() {

		// Create a stochastic random variable
		final RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		final RandomVariable randomVariable2 = randomVariableFactory.createRandomVariable(0.0,
				new double[] {-3.0, -3.0, -3.0, -3.0, -3.0} );

		final RandomVariable valueCapped = randomVariable.cap(randomVariable2);

		// The random variable has average value 3.0
		Assert.assertEquals(valueCapped.getAverage(), -3.0 - 1.0/5, 1E-15);

		// The random variable has variance value 0
		Assert.assertEquals(valueCapped.getVariance(), Math.pow(1.0/5.0,2)*4.0/5.0 + Math.pow(1.0-1.0/5-0,2)*1.0/5.0, 1E-15);
	}

	@Test
	public void testFloor() {

		// Create a stochastic random variable
		final RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		final RandomVariable randomVariable2 = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 3.0, 3.0, 3.0, 3.0} );

		final RandomVariable valueFloored = randomVariable.floor(randomVariable2);

		// The random variable has average value 3.0
		Assert.assertEquals(valueFloored.getAverage(), 3.0 + 1.0/5, 1E-15);

		// The random variable has variance value 0
		Assert.assertEquals(valueFloored.getVariance(), Math.pow(1.0/5.0,2)*4.0/5.0 + Math.pow(1.0-1.0/5-0,2)*1.0/5.0, 1E-15);
	}
}
