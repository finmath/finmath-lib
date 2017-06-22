/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 03.06.2017
 */
package net.finmath.montecarlo.automaticdifferentiation.backward.alternative;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.automaticdifferentiation.backward.alternative.RandomVariableAAD;
import net.finmath.montecarlo.automaticdifferentiation.backward.alternative.RandomVariableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.backward.alternative.RandomVariableAADFactory.RandomVariableWithAAD;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Test cases for the class net.finmath.montecarlo.RandomVariableAAD.
 * 
 * @author Christian Fries
 * @author Stefan Sedlmair
 * @see net.finmath.montecarlo.RandomVariable
 */
public class RandomVariableAADTest {

	@Test
	public void testRandomVariableDeterministc() {

		RandomVariableAAD.resetArrayListOfAllAADRandomVariables();

		
		// Create a random variable with a constant
		RandomVariableInterface randomVariable = RandomVariableAAD.constructNewAADRandomVariable(2.0);
		
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

		RandomVariableAAD.resetArrayListOfAllAADRandomVariables();

		// Create a stochastic random variable
		RandomVariableInterface randomVariable2 = RandomVariableAAD.constructNewAADRandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		// Perform some calculations
		randomVariable2 = randomVariable2.add(4.0);
		randomVariable2 = randomVariable2.div(2.0);
		
		// The random variable has average value 2.0
		Assert.assertTrue(randomVariable2.getAverage() == 2.0);

		// The random variable has variance value 2.0 = (4 + 1 + 0 + 1 + 4) / 5
		Assert.assertEquals(2.0, randomVariable2.getVariance(), 1E-12);
		
		// Multiply two random variables, this will expand the receiver to a stochastic one
		RandomVariableInterface randomVariable = RandomVariableAAD.constructNewAADRandomVariable(3.0);
		randomVariable = randomVariable.mult(randomVariable2);
		
		// The random variable has average value 6.0
		Assert.assertTrue(randomVariable.getAverage() == 6.0);

		// The random variable has variance value 2 * 9
		Assert.assertTrue(randomVariable.getVariance() == 2.0 * 9.0);
	}

	@Test
	public void testRandomVariableArithmeticSqrtPow() {

		RandomVariableAAD.resetArrayListOfAllAADRandomVariables();
		
		// Create a stochastic random variable
		RandomVariableInterface randomVariable = RandomVariableAAD.constructNewAADRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.sqrt().sub(randomVariable.pow(0.5));
		
		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);
		
	}

	@Test
	public void testRandomVariableArithmeticSquaredPow() {

		RandomVariableAAD.resetArrayListOfAllAADRandomVariables();

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = RandomVariableAAD.constructNewAADRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.squared().sub(randomVariable.pow(2.0));
		
		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);
		
	}

	@Test
	public void testRandomVariableStandardDeviation() {

		RandomVariableAAD.resetArrayListOfAllAADRandomVariables();
		
		// Create a stochastic random variable
		RandomVariableInterface randomVariable = RandomVariableAAD.constructNewAADRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		double check = randomVariable.getStandardDeviation() - Math.sqrt(randomVariable.getVariance());
		Assert.assertTrue(check == 0.0);
	}
	
	@Test
	public void testRandomVariableSimpleGradient(){
		
		RandomVariableAAD.resetArrayListOfAllAADRandomVariables();
		
		RandomVariable randomVariable01 = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0});
		RandomVariable randomVariable02 = new RandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );
		
		/*x_1*/
		RandomVariableAAD aadRandomVariable01 = RandomVariableAAD.constructNewAADRandomVariable(randomVariable01);
		
		/*x_2*/
		RandomVariableAAD aadRandomVariable02 = RandomVariableAAD.constructNewAADRandomVariable(randomVariable02);
		
		/* x_3 = x_1 + x_2 */
		RandomVariableInterface aadRandomVariable03 = aadRandomVariable01.add(aadRandomVariable02);
		/* x_4 = x_3 * x_1 */
		RandomVariableInterface aadRandomVariable04 = aadRandomVariable03.mult(aadRandomVariable01);
		/* x_5 = x_4 + x_1 = ((x_1 + x_2) * x_1) + x_1 = x_1^2 + x_2x_1 + x_1*/
		RandomVariableInterface aadRandomVariable05 = aadRandomVariable04.add(aadRandomVariable01);
		
		Map<Integer, RandomVariableInterface> aadGradient = ((RandomVariableAAD)aadRandomVariable05).getGradient();
		
		/* dy/dx_1 = x_1 * 2 + x_2 + 1
		 * dy/dx_2 = x_1 */
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{
				randomVariable01.mult(2.0).add(randomVariable02).add(1.0),
				randomVariable01
		};
		
		for(int i=0; i<analyticGradient.length;i++){
			System.out.println(analyticGradient[i]);
			System.out.println(aadGradient.get(i));
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(i)));
		}
	}

	@Test
	public void testRandomVariableSimpleGradient2(){
		
		RandomVariableAADFactory randomVariableFactory = new RandomVariableAADFactory();
		
		RandomVariable randomVariable01 = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0});
		RandomVariable randomVariable02 = new RandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );
		
		/*x_1*/
		RandomVariableWithAAD aadRandomVariable01 = randomVariableFactory.constructNewAADRandomVariable(randomVariable01);
		
		/*x_2*/
		RandomVariableWithAAD aadRandomVariable02 = randomVariableFactory.constructNewAADRandomVariable(randomVariable02);
		
		/* x_3 = x_1 + x_2 */
		RandomVariableInterface aadRandomVariable03 = aadRandomVariable01.add(aadRandomVariable02);
		/* x_4 = x_3 * x_1 */
		RandomVariableInterface aadRandomVariable04 = aadRandomVariable03.mult(aadRandomVariable01);
		/* x_5 = x_4 + x_1 = ((x_1 + x_2) * x_1) + x_1 = x_1^2 + x_2x_1 + x_1*/
		RandomVariableInterface aadRandomVariable05 = aadRandomVariable04.add(aadRandomVariable01);
		
		Map<Integer, RandomVariableInterface> aadGradient = ((RandomVariableWithAAD)aadRandomVariable05).getGradient();
		
		/* dy/dx_1 = x_1 * 2 + x_2 + 1
		 * dy/dx_2 = x_1 */
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{
				randomVariable01.mult(2.0).add(randomVariable02).add(1.0),
				randomVariable01
		};
		
		for(int i=0; i<analyticGradient.length;i++){
			System.out.println(analyticGradient[i]);
			System.out.println(aadGradient.get(i));
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(i)));
		}
	}

	@Test
	public void testRandomVariableGradientBigSum(){

		RandomVariableAAD.resetArrayListOfAllAADRandomVariables();
		
		/* OutOfMemoryError for >= 10^6*/
		int lengthOfVectors = 4 * (int) Math.pow(10, 5);
		
		double[] x = new double[lengthOfVectors];
		
		for(int i=0; i < lengthOfVectors; i++){
			x[i] = Math.random();
		}
				
		RandomVariable randomVariable01 = new RandomVariable(0.0, x);
		
		/*x_1*/
		RandomVariableAAD aadRandomVariable01 = RandomVariableAAD.constructNewAADRandomVariable(randomVariable01);

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);
		
		RandomVariableAAD sum = RandomVariableAAD.constructNewAADRandomVariable(0.0);
		for(int i = 0; i < numberOfIterations; i++){
			sum = (RandomVariableAAD) sum.add(aadRandomVariable01);
		}
		
		Map<Integer, RandomVariableInterface> aadGradient = sum.getGradient();
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{new RandomVariable(numberOfIterations)};
		
		for(int i=0; i<analyticGradient.length;i++){
			System.out.println(i + "\t" + aadGradient.get(i));
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(i)));
		}
		
	}

	@Test
	public void testRandomVariableGradientBigSum2(){

		/* OutOfMemoryError for >= 10^6*/
		int lengthOfVectors = 4 * (int) Math.pow(10, 4);
		
		double[] x = new double[lengthOfVectors];
		
		for(int i=0; i < lengthOfVectors; i++){
			x[i] = Math.random();
		}
				
		RandomVariable randomVariable01 = new RandomVariable(0.0, x);
		RandomVariable randomVariable02 = new RandomVariable(0.0, x);

		RandomVariableAADFactory randomVariableFactory = new RandomVariableAADFactory();

		/*x_1*/
		RandomVariableWithAAD aadRandomVariable01 = randomVariableFactory.constructNewAADRandomVariable(randomVariable01);

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);
		
		RandomVariableInterface sum = randomVariableFactory.constructNewAADRandomVariable(0.0);
		for(int i = 0; i < numberOfIterations; i++){
			sum = sum.add(aadRandomVariable01);
		}
		
		Map<Integer, RandomVariableInterface> aadGradient = ((RandomVariableWithAAD)sum).getGradient();
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{new RandomVariable(numberOfIterations)};
		
		for(int i=0; i<analyticGradient.length;i++){
			System.out.println(i + "\t" + aadGradient.get(i));
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(i)));
		}
		
	}

	@Test
	public void testRandomVariableGradientBigSumWithConstants(){

		/* OutOfMemoryError for >= 10^6*/
		int lengthOfVectors = 4 * (int) Math.pow(10, 4);

		// Generate some random Vector
		double[] x = new double[lengthOfVectors];
		for(int i=0; i < lengthOfVectors; i++) x[i] = Math.random();
				
		RandomVariable randomVariable01 = new RandomVariable(0.0, x);
		RandomVariable randomVariable02 = new RandomVariable(0.0, x);

		RandomVariableAADFactory randomVariableFactory = new RandomVariableAADFactory();

		/*x_1*/
		RandomVariableWithAAD aadRandomVariable01 = randomVariableFactory.constructNewAADRandomVariable(randomVariable01);

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);

		/*
		 * sum = \Sigma_{i=0}^{n-1} (x_1 + a)
		 * Note: we like to differentiate with respect to x_1, that is, a should have no effect!
		 */
		RandomVariableInterface sum = randomVariableFactory.constructNewAADRandomVariable(0.0);
		for(int i = 0; i < numberOfIterations; i++){
			sum = sum.add(aadRandomVariable01);
			sum = sum.add(randomVariable02);
		}
		
		Map<Integer, RandomVariableInterface> aadGradient = ((RandomVariableWithAAD)sum).getGradient();
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{new RandomVariable(numberOfIterations)};
		
		for(int i=0; i<analyticGradient.length;i++){
			System.out.println(i + "\t" + aadGradient.get(i));
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(i)));
		}
		
	}}
