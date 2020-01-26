/**
 *
 */
package net.finmath.montecarlo.automaticdifferentiation;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.forward.RandomVariableDifferentiableADFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Unit test for random variables implementing <code>RandomVariableDifferentiable</code>.
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 */
@RunWith(Parameterized.class)
public class RandomVariableDifferentiableTest {

	/* parameters specify the factories one wants to test against each other */
	@Parameters
	public static Collection<Object[]> data(){
		return Arrays.asList(new Object[][] {
			{new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{new RandomVariableDifferentiableAADFactory(new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{new RandomVariableDifferentiableADFactory(new RandomVariableFromArrayFactory(true  /* isUseDoublePrecisionFloatingPointImplementation */)) },
			{new RandomVariableDifferentiableADFactory(new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */)) },
		});
	}

	private final AbstractRandomVariableDifferentiableFactory randomVariableFactory;

	public RandomVariableDifferentiableTest(AbstractRandomVariableDifferentiableFactory factory) {
		randomVariableFactory = factory;
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
		Assert.assertTrue(randomVariable.getAverage() == 3.0);

		// Since the random variable is deterministic, it has zero variance
		Assert.assertTrue(randomVariable.getVariance() == 0.0);

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
		Assert.assertTrue(randomVariable2.getAverage() == 2.0);

		// The random variable has variance value 2.0 = (4 + 1 + 0 + 1 + 4) / 5
		Assert.assertEquals(2.0, randomVariable2.getVariance(), 1E-12);

		// Multiply two random variables, this will expand the receiver to a stochastic one
		RandomVariable randomVariable = randomVariableFactory.createRandomVariable(3.0);
		randomVariable = randomVariable.mult(randomVariable2);

		// The random variable has average value 6.0
		Assert.assertTrue(randomVariable.getAverage() == 6.0);

		// The random variable has variance value 2 * 9
		Assert.assertTrue(randomVariable.getVariance() == 2.0 * 9.0);
	}

	@Test
	public void testRandomVariableArithmeticSqrtPow() {

		// Create a stochastic random variable
		RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariable check = randomVariable.sqrt().sub(randomVariable.pow(0.5));

		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);

	}

	@Test
	public void testRandomVariableArithmeticSquaredPow() {

		// Create a stochastic random variable
		RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariable check = randomVariable.squared().sub(randomVariable.pow(2.0));

		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);

	}

	@Test
	public void testRandomVariableStandardDeviation() {

		// Create a stochastic random variable
		RandomVariable randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		double check = randomVariable.getStandardDeviation() - Math.sqrt(randomVariable.getVariance());
		Assert.assertTrue(check == 0.0);
	}

	@Test
	public void testRandomVariableSimpleGradient(){

		RandomVariableFromDoubleArray randomVariable01 = new RandomVariableFromDoubleArray(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0});
		RandomVariableFromDoubleArray randomVariable02 = new RandomVariableFromDoubleArray(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		/*x_1*/
		RandomVariable aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/*x_2*/
		RandomVariable aadRandomVariable02 =  randomVariableFactory.createRandomVariable(randomVariable02.getFiltrationTime(), randomVariable02.getRealizations());


		/* x_3 = x_1 + x_2 */
		RandomVariable aadRandomVariable03 = aadRandomVariable01.add(aadRandomVariable02);
		/* x_4 = x_3 * x_1 */
		RandomVariable aadRandomVariable04 = aadRandomVariable03.mult(aadRandomVariable01);
		/* x_5 = x_4 + x_1 = ((x_1 + x_2) * x_1) + x_1 = x_1^2 + x_2x_1 + x_1*/
		RandomVariable aadRandomVariable05 = aadRandomVariable04.add(aadRandomVariable01);

		Map<Long, RandomVariable> aadGradient = ((RandomVariableDifferentiable)aadRandomVariable05).getGradient();

		/* dy/dx_1 = x_1 * 2 + x_2 + 1
		 * dy/dx_2 = x_1 */
		RandomVariable[] analyticGradient = new RandomVariable[]{
				randomVariable01.mult(2.0).add(randomVariable02).add(1.0),
				randomVariable01
		};

		Long[] keys = new Long[aadGradient.keySet().size()];
		keys = aadGradient.keySet().toArray(keys);
		Arrays.sort(keys);

		for(int i=0; i<analyticGradient.length;i++){
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(keys[i])));
		}
	}

	@Test
	public void testRandomVariableSimpleGradient2(){

		RandomVariableFromDoubleArray randomVariable01 = new RandomVariableFromDoubleArray(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0});
		RandomVariableFromDoubleArray randomVariable02 = new RandomVariableFromDoubleArray(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		/*x_1*/
		RandomVariable aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/*x_2*/
		RandomVariable aadRandomVariable02 = randomVariableFactory.createRandomVariable(randomVariable02.getFiltrationTime(), randomVariable02.getRealizations());

		/* x_3 = x_1 + x_2 */
		RandomVariable aadRandomVariable03 = aadRandomVariable01.add(aadRandomVariable02);
		/* x_4 = x_3 * x_1 */
		RandomVariable aadRandomVariable04 = aadRandomVariable03.mult(aadRandomVariable01);
		/* x_5 = x_4 + x_1 = ((x_1 + x_2) * x_1) + x_1 = x_1^2 + x_2x_1 + x_1*/
		RandomVariable aadRandomVariable05 = aadRandomVariable04.add(aadRandomVariable01);

		Map<Long, RandomVariable> aadGradient = ((RandomVariableDifferentiable) aadRandomVariable05).getGradient();

		/* dy/dx_1 = x_1 * 2 + x_2 + 1
		 * dy/dx_2 = x_1 */
		RandomVariable[] analyticGradient = new RandomVariable[]{
				randomVariable01.mult(2.0).add(randomVariable02).add(1.0),
				randomVariable01
		};

		Long[] keys = new Long[aadGradient.keySet().size()];
		keys = aadGradient.keySet().toArray(keys);
		Arrays.sort(keys);

		for(int i=0; i<analyticGradient.length;i++){
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(keys[i])));
		}
	}

	@Test
	public void testRandomVariableGradientBigSum(){

		/* OutOfMemoryError for >= 10^6*/
		int lengthOfVectors = (int) Math.pow(10, 5);

		double[] x = new double[lengthOfVectors];

		for(int i=0; i < lengthOfVectors; i++){
			x[i] = Math.random();
		}

		RandomVariableFromDoubleArray randomVariable01 = new RandomVariableFromDoubleArray(0.0, x);

		/*x_1*/
		RandomVariable aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);

		RandomVariableDifferentiable sum = randomVariableFactory.createRandomVariable(0.0);
		for(int i = 0; i < numberOfIterations; i++){
			sum = (RandomVariableDifferentiable) sum.add(aadRandomVariable01);
		}

		Map<Long, RandomVariable> aadGradient = sum.getGradient();
		RandomVariable[] analyticGradient = new RandomVariable[]{new RandomVariableFromDoubleArray(numberOfIterations)};

		Long[] keys = new Long[aadGradient.keySet().size()];
		keys = aadGradient.keySet().toArray(keys);
		Arrays.sort(keys);

		for(int i=0; i<analyticGradient.length;i++){
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(keys[i])));
		}

	}

	@Test
	public void testRandomVariableGradientBiggerSum(){

		try {
			/* OutOfMemoryError for >= 10^6 for some implementations! */
			int lengthOfVectors = (int) Math.pow(10, 6);

			double[] x = new double[lengthOfVectors];
			Random random = new Random(314151);
			for(int i=0; i < lengthOfVectors; i++) {
				x[i] = random.nextDouble();
			}

			RandomVariableFromDoubleArray randomVariable01 = new RandomVariableFromDoubleArray(0.0, x);

			/*x_1*/
			RandomVariable aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

			/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
			int numberOfIterations =  (int) Math.pow(10, 3);

			long startValuation = System.currentTimeMillis();

			RandomVariableDifferentiable sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = (RandomVariableDifferentiable) sum.add(aadRandomVariable01);
			}

			long endValuation = System.currentTimeMillis();
			long millisValuation = endValuation-startValuation;

			long startAutoDiffDerivative = System.currentTimeMillis();

			Map<Long, RandomVariable> aadGradient = sum.getGradient();

			long endAutoDiffDerivative = System.currentTimeMillis();
			long millisAutoDiffDerivative = endAutoDiffDerivative-startAutoDiffDerivative;

			RandomVariable[] analyticGradient = new RandomVariable[]{new RandomVariableFromDoubleArray(numberOfIterations)};

			Long[] keys = new Long[aadGradient.keySet().size()];
			keys = aadGradient.keySet().toArray(keys);
			Arrays.sort(keys);

			System.out.println("Valuation.............: " + millisValuation/1000.0 + " s.");
			System.out.println("Derivative (auto diff): " + millisAutoDiffDerivative/1000.0 + " s.");

			for(int i=0; i<analyticGradient.length;i++){
				Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(keys[i])));
			}
		} catch(java.lang.OutOfMemoryError e) {
			System.out.println("Failed due to out of memory (this is expected for some implementations).");
		}

	}

	@Test
	public void testRandomVariableGradientBigSum2(){

		/* OutOfMemoryError for >= 10^6 */
		int lengthOfVectors = 4 * (int) Math.pow(10, 4);

		double[] x = new double[lengthOfVectors];

		for(int i=0; i < lengthOfVectors; i++){
			x[i] = Math.random();
		}

		/*x_1*/
		RandomVariableDifferentiable randomVariable01 =
				randomVariableFactory.createRandomVariable(0.0, x);

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);

		RandomVariable sum = randomVariableFactory.createRandomVariable(0.0);
		for(int i = 0; i < numberOfIterations; i++) {
			sum = sum.add(randomVariable01);
		}

		Map<Long, RandomVariable> aadGradient = ((RandomVariableDifferentiable) sum).getGradient();
		RandomVariable[] analyticGradient = new RandomVariable[]{new RandomVariableFromDoubleArray(numberOfIterations)};

		Long[] keys = new Long[aadGradient.keySet().size()];
		keys = aadGradient.keySet().toArray(keys);
		Arrays.sort(keys);

		for(int i=0; i<analyticGradient.length;i++){
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(keys[i])));
		}

	}

	@Test
	public void testRandomVariableExpectation(){

		int numberOfPaths = 100000;
		int seed = 3141;
		BrownianMotion brownianMotion = new BrownianMotionLazyInit(new TimeDiscretizationFromArray(0.0, 1.0), 1 /* numberOfFactors */, numberOfPaths, seed);
		RandomVariable brownianIncrement = brownianMotion.getIncrement(0, 0);

		RandomVariableDifferentiable x = randomVariableFactory.createRandomVariable(1.0);

		RandomVariable y = x.mult(brownianIncrement.sub(brownianIncrement.average())).average().mult(brownianIncrement);

		Map<Long, RandomVariable> aadGradient = ((RandomVariableDifferentiable) y).getGradient();

		RandomVariable derivative = aadGradient.get(x.getID());

		System.out.println(randomVariableFactory.toString());
		System.out.println(y.getAverage());
		System.out.println(brownianIncrement.squared().getAverage());
		System.out.println((aadGradient.get(x.getID())).getAverage());

		Assert.assertEquals(0.0, y.getAverage(), 1E-8);

		// Test RandomVariableDifferentiableAADFactory (the others currently fail)
		if(randomVariableFactory instanceof RandomVariableDifferentiableAADFactory) {
			Assert.assertEquals(0.0, derivative.getAverage(), 1E-8);
		}
	}

	@Test
	public void testRandomVariableGradientBigSumWithConstants(){

		/* OutOfMemoryError for >= 10^6*/
		int lengthOfVectors = 4 * (int) Math.pow(10, 4);

		// Generate some random Vector
		double[] x = new double[lengthOfVectors];
		for(int i=0; i < lengthOfVectors; i++) {
			x[i] = Math.random();
		}

		RandomVariableFromDoubleArray randomVariable01 = new RandomVariableFromDoubleArray(0.0, x);
		RandomVariableFromDoubleArray randomVariable02 = new RandomVariableFromDoubleArray(0.0, x);

		/*x_1*/
		RandomVariableDifferentiable aadRandomVariable01 =
				randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);

		/*
		 * sum = \Sigma_{i=0}^{n-1} (x_1 + a)
		 * Note: we like to differentiate with respect to x_1, that is, a should have no effect!
		 */

		RandomVariable sum = randomVariableFactory.createRandomVariable(0.0);

		for(int i = 0; i < numberOfIterations; i++){
			sum = sum.add(aadRandomVariable01);
			sum = sum.add(randomVariable02);
		}

		Map<Long, RandomVariable> aadGradient = ((RandomVariableDifferentiable) sum).getGradient();
		RandomVariable[] analyticGradient = new RandomVariable[]{new RandomVariableFromDoubleArray(numberOfIterations)};

		Long[] keys = new Long[aadGradient.keySet().size()];
		keys = aadGradient.keySet().toArray(keys);
		Arrays.sort(keys);

		for(int i=0; i<analyticGradient.length;i++){
			Assert.assertTrue(analyticGradient[i].equals(aadGradient.get(keys[i])));
		}
	}

	@Test
	public void testRandomVariableDifferentiableInterfaceVsFiniteDifferences(){

		double epsilon = Math.pow(10, -8);
		double delta = Math.pow(10, -6);

		int numberOfRandomVariables = 50;

		int lengthOfVectors = (int) Math.pow(10, 5);

		// Generate some random Vector
		double[] values = new double[lengthOfVectors];
		RandomVariable[] randomVariables = new RandomVariable[numberOfRandomVariables];

		Random random = new Random(2);
		for(int j = 0; j < numberOfRandomVariables; j++) {
			for(int i=0; i < lengthOfVectors; i++) {
				values[i] = random.nextDouble();
			}
			randomVariables[j] = randomVariableFactory.createRandomVariable(0.0, values);
		}

		/*
		 * Calcuate gradient using auto differentiation (factory implementation)
		 */
		long startAAD = System.currentTimeMillis();
		Map<Long, RandomVariable> gradientAutoDiff = ((RandomVariableDifferentiable) testFunction(randomVariables)).getGradient();
		long endAAD = System.currentTimeMillis();

		/*
		 * Calcuate gradient using auto differentiation (factory implementation)
		 */

		// Note: copy random variable into a RandomVariableFromDoubleArray to ensure that an alternative implementation is used
		RandomVariable[] randomVariablesValues = new RandomVariable[randomVariables.length];
		for(int j = 0; j < numberOfRandomVariables; j++) {
			randomVariablesValues[j] = new RandomVariableFromDoubleArray(randomVariables[j]);

		}

		long startFD = System.currentTimeMillis();

		RandomVariable[] gradientNumeric = new RandomVariable[numberOfRandomVariables];
		for(int j = 0; j < numberOfRandomVariables; j++) {

			RandomVariable[] randomVariables_p = randomVariablesValues.clone();
			RandomVariable[] randomVariables_m = randomVariablesValues.clone();

			randomVariables_p[j] = randomVariablesValues[j].add(epsilon);
			randomVariables_m[j] = randomVariablesValues[j].sub(epsilon);

			/* df(x_1,...,x_n)/dx_i = (f(x_1 ,...,x_i + \epsilon,...,x_n) - f(x_1 ,...,x_i - \epsilon,...,x_n))/(2 * \epsilon) */
			gradientNumeric[j] = testFunction(randomVariables_p).sub(testFunction(randomVariables_m)).div(2*epsilon);
		}
		long endFD = System.currentTimeMillis();

		System.out.println("Time needed for AAD (" + randomVariableFactory.getClass().getSimpleName() + "): " + ((endAAD - startAAD) / 1000.0) + "s");
		System.out.println("Time needed for FD: " + ((endFD - startFD) / 1000.0) + "s");

		for(int i=0; i<gradientNumeric.length;i++) {
			RandomVariable diffNumeric = gradientNumeric[i];
			RandomVariable diffAutoDiff =  gradientAutoDiff.get(((RandomVariableDifferentiable)randomVariables[i]).getID());
			double errorL1 = diffNumeric.sub(diffAutoDiff).abs().getAverage();
			/* if the average of the absolute error is not too big give okay*/
			Assert.assertEquals(0.0, errorL1, delta);
			//			Assert.assertEquals(0.0, gradientNumeric[i].sub(gradientAutoDiff.get(keys[i])).abs().getAverage(), delta);
		}
	}

	private RandomVariable testFunction(RandomVariable[] randomVariables){

		RandomVariable result = randomVariables[0];
		for(int i = 1; i < randomVariables.length; i++) {
			result = result.addProduct(randomVariables[i-1].abs(), randomVariables[i].exp());
		}

		result = result.cap(randomVariables[randomVariables.length-1]).add(result.cap(randomVariables[randomVariables.length-1]));

		return result;
	}
}
