package net.finmath.montecarlo

import net.finmath.montecarlo.RandomVariable
import net.finmath.stochastic.RandomVariableInterface
import org.junit.Assert
import org.junit.Test


class RandomVariableKotlinTest {
	@Test
	fun testRandomVariableDeterministc() {

		// Create a random variable with a constant
		val randomVariable : RandomVariableInterface = RandomVariable(2.0)

		// Perform some calculations
		val result = randomVariable
				.mult(2.0)
				.add(1.0)
				.squared()
				.sub(4.0)
				.div(7.0)

		// The random variable has average value 3.0 (it is constant 3.0)
		Assert.assertTrue(result.getAverage() == 3.0)
	
		// Since the random variable is deterministic, it has zero variance
		Assert.assertTrue(result.getVariance() == 0.0)
	}

	@Test
	fun testRandomVariableStochastic() {

		// Create a stochastic random variable
		var randomVariable2 : RandomVariableInterface = RandomVariable(0.0, doubleArrayOf(-4.0, -2.0, 0.0, 2.0, 4.0) );

		// Perform some calculations
		randomVariable2 = randomVariable2.add(4.0);
		randomVariable2 = randomVariable2.div(2.0);
	
		// The random variable has average value 2.0
		Assert.assertTrue(randomVariable2.getAverage() == 2.0);
	
		// The random variable has variance value 2.0 = (4 + 1 + 0 + 1 + 4) / 5
		Assert.assertEquals(2.0, randomVariable2.getVariance(), 1E-12);
	
		// Multiply two random variables, this will expand the receiver to a stochastic one
		var randomVariable : RandomVariableInterface = RandomVariable(3.0);
		randomVariable = randomVariable.mult(randomVariable2);
	
		// The random variable has average value 6.0
		Assert.assertTrue(randomVariable.getAverage() == 6.0);
	
		// The random variable has variance value 2 * 9
		Assert.assertTrue(randomVariable.getVariance() == 2.0 * 9.0);
	}
}
