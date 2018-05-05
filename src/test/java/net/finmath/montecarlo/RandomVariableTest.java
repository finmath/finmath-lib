/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo;

import org.apache.commons.math3.random.MersenneTwister;
import org.junit.Assert;
import org.junit.Test;

import net.finmath.stochastic.RandomVariableInterface;

/**
 * Test cases for the class net.finmath.montecarlo.RandomVariable.
 * 
 * @author Christian Fries
 * @see net.finmath.montecarlo.RandomVariable
 */
public class RandomVariableTest {

	@Test
	public void testRandomVariableDeterministc() {

		// Create a random variable with a constant
		RandomVariableInterface randomVariable = new RandomVariable(2.0);
		
		// Perform some calculations
		randomVariable = randomVariable.mult(2.0);
		randomVariable = randomVariable.add(1.0);
		randomVariable = randomVariable.squared();
		randomVariable = randomVariable.sub(4.0);
		randomVariable = randomVariable.div(7.0);
		
		// The random variable has average value 3.0 (it is constant 3.0)
		Assert.assertTrue(randomVariable.getAverage() == 3.0);
		
		// Since the random variable is deterministic, it has zero variance
		Assert.assertTrue(randomVariable.getVariance() == 0.0);
	}

	@Test
	public void testRandomVariableStochastic() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable2 = new RandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		// Perform some calculations
		randomVariable2 = randomVariable2.add(4.0);
		randomVariable2 = randomVariable2.div(2.0);
		
		// The random variable has average value 2.0
		Assert.assertTrue(randomVariable2.getAverage() == 2.0);

		// The random variable has variance value 2.0 = (4 + 1 + 0 + 1 + 4) / 5
		Assert.assertEquals(2.0, randomVariable2.getVariance(), 1E-12);
		
		// Multiply two random variables, this will expand the receiver to a stochastic one
		RandomVariableInterface randomVariable = new RandomVariable(3.0);
		randomVariable = randomVariable.mult(randomVariable2);
		
		// The random variable has average value 6.0
		Assert.assertTrue(randomVariable.getAverage() == 6.0);

		// The random variable has variance value 2 * 9
		Assert.assertTrue(randomVariable.getVariance() == 2.0 * 9.0);
	}

	@Test
	public void testRandomVariableArithmeticSqrtPow() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.sqrt().sub(randomVariable.pow(0.5));
		
		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);
	}

	@Test
	public void testRandomVariableArithmeticSquaredPow() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.squared().sub(randomVariable.pow(2.0));
		
		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);
	}

	@Test
	public void testRandomVariableStandardDeviation() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		double check = randomVariable.getStandardDeviation() - Math.sqrt(randomVariable.getVariance());
		Assert.assertTrue(check == 0.0);
	}
	
	/**
	 * Testing quantiles of normal distribution.
	 * Based on feedback provided by Alessandro Gnoatto and a student of him.
	 */
	@Test
	public void testGetQuantile() {
		
		final int seed = 3141;
		final int numberOfSamplePoints = 10000000;
		
		MersenneTwister mersenneTwister = new MersenneTwister(seed);
		double[] samples = new double[numberOfSamplePoints];
		for(int i = 0; i< numberOfSamplePoints; i++) {
			double randomNumber = mersenneTwister.nextDouble();
			samples[i] = net.finmath.functions.NormalDistribution.inverseCumulativeDistribution(randomNumber);
		}
		
		RandomVariableInterface normalDistributedRandomVariable = new RandomVariable(0.0,samples);
		
		double q00 = normalDistributedRandomVariable.getQuantile(0.0);
		Assert.assertEquals(normalDistributedRandomVariable.getMin(), q00, 1E-12);

		double q05 = normalDistributedRandomVariable.getQuantile(0.05);
		Assert.assertEquals(-1.645, q05, 1E-3);

		double q50 = normalDistributedRandomVariable.getQuantile(0.5);
		Assert.assertEquals(0, q50, 2E-4);

		double q95 = normalDistributedRandomVariable.getQuantile(0.95);
		Assert.assertEquals(1.645, q95, 1E-3);

		double q99 = normalDistributedRandomVariable.getQuantile(0.99);
		Assert.assertEquals(2.33, q99, 1E-2);
	}
}
