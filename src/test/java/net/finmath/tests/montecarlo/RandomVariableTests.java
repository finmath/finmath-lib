/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.tests.montecarlo;

import static org.junit.Assert.assertTrue;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;

import org.junit.Test;

/**
 * Test cases for the class net.finmath.montecarlo.RandomVariable.
 * 
 * @author Christian Fries
 * @see net.finmath.montecarlo.RandomVariable
 */
public class RandomVariableTests {

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
		assertTrue(randomVariable.getAverage() == 3.0);
		
		// Since the random variable is deterministic, it has zero variance
		assertTrue(randomVariable.getVariance() == 0.0);
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
		assertTrue(randomVariable2.getAverage() == 2.0);

		// The random variable has variance value 2.0 = (4 + 1 + 0 + 1 + 4) / 5
		assertTrue(randomVariable2.getVariance() == 2.0);
		
		// Multiply two random variables, this will expand the receiver to a stochastic one
		RandomVariableInterface randomVariable = new RandomVariable(3.0);
		randomVariable = randomVariable.mult(randomVariable2);
		
		// The random variable has average value 6.0
		assertTrue(randomVariable.getAverage() == 6.0);

		// The random variable has variance value 2 * 9
		assertTrue(randomVariable.getVariance() == 2.0 * 9.0);
	}

	@Test
	public void testRandomVariableArithmeticSqrtPow() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.sqrt().sub(randomVariable.pow(0.5));
		
		// The random variable is identical 0.0
		assertTrue(check.getAverage() == 0.0);
		assertTrue(check.getVariance() == 0.0);
	}

	@Test
	public void testRandomVariableArithmeticSquaredPow() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.squared().sub(randomVariable.pow(2.0));
		
		// The random variable is identical 0.0
		assertTrue(check.getAverage() == 0.0);
		assertTrue(check.getVariance() == 0.0);
	}

	@Test
	public void testRandomVariableStandardDeviation() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		double check = randomVariable.getStandardDeviation() - Math.sqrt(randomVariable.getVariance());
		assertTrue(check == 0.0);
	}
}
