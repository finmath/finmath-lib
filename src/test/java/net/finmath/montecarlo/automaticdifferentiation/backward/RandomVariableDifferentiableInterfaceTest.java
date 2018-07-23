/**
 *
 */
package net.finmath.montecarlo.automaticdifferentiation.backward;

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
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.automaticdifferentiation.AbstractRandomVariableDifferentiableFactory;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;

/**
 * Unit test for random variables implementing <code>RandomVariableDifferentiableInterface</code>.
 *
 * @author Christian Fries
 * @author Stefan Sedlmair
 */
@RunWith(Parameterized.class)
public class RandomVariableDifferentiableInterfaceTest {

	/* parameters specify the factories one wants to test against each other */
	@Parameters
	public static Collection<Object[]> data(){
		return Arrays.asList(new Object[][] {
			{new RandomVariableDifferentiableAADFactory(new RandomVariableFactory()) },
		});
	}

	private final AbstractRandomVariableDifferentiableFactory randomVariableFactory;

	public RandomVariableDifferentiableInterfaceTest(AbstractRandomVariableDifferentiableFactory factory) {
		this.randomVariableFactory = factory;
	}

	@Test
	public void testRandomVariableDeterministc() {

		// Create a random variable with a constant
		RandomVariableInterface randomVariable = randomVariableFactory.createRandomVariable(2.0);

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
		RandomVariableInterface randomVariable2 = randomVariableFactory.createRandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		// Perform some calculations
		randomVariable2 = randomVariable2.add(4.0);
		randomVariable2 = randomVariable2.div(2.0);

		// The random variable has average value 2.0
		Assert.assertTrue(randomVariable2.getAverage() == 2.0);

		// The random variable has variance value 2.0 = (4 + 1 + 0 + 1 + 4) / 5
		Assert.assertEquals(2.0, randomVariable2.getVariance(), 1E-12);

		// Multiply two random variables, this will expand the receiver to a stochastic one
		RandomVariableInterface randomVariable = randomVariableFactory.createRandomVariable(3.0);
		randomVariable = randomVariable.mult(randomVariable2);

		// The random variable has average value 6.0
		Assert.assertTrue(randomVariable.getAverage() == 6.0);

		// The random variable has variance value 2 * 9
		Assert.assertTrue(randomVariable.getVariance() == 2.0 * 9.0);
	}

	@Test
	public void testRandomVariableArithmeticSqrtPow() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.sqrt().sub(randomVariable.pow(0.5));

		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);

	}

	@Test
	public void testRandomVariableArithmeticSquaredPow() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		RandomVariableInterface check = randomVariable.squared().sub(randomVariable.pow(2.0));

		// The random variable is identical 0.0
		Assert.assertTrue(check.getAverage() == 0.0);
		Assert.assertTrue(check.getVariance() == 0.0);

	}

	@Test
	public void testRandomVariableStandardDeviation() {

		// Create a stochastic random variable
		RandomVariableInterface randomVariable = randomVariableFactory.createRandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0, 1.0/3.0} );

		double check = randomVariable.getStandardDeviation() - Math.sqrt(randomVariable.getVariance());
		Assert.assertTrue(check == 0.0);
	}

	@Test
	public void testRandomVariableSimpleGradient(){

		RandomVariable randomVariable01 = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0});
		RandomVariable randomVariable02 = new RandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		/*x_1*/
		RandomVariableInterface aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/*x_2*/
		RandomVariableInterface aadRandomVariable02 =  randomVariableFactory.createRandomVariable(randomVariable02.getFiltrationTime(), randomVariable02.getRealizations());


		/* x_3 = x_1 + x_2 */
		RandomVariableInterface aadRandomVariable03 = aadRandomVariable01.add(aadRandomVariable02);
		/* x_4 = x_3 * x_1 */
		RandomVariableInterface aadRandomVariable04 = aadRandomVariable03.mult(aadRandomVariable01);
		/* x_5 = x_4 + x_1 = ((x_1 + x_2) * x_1) + x_1 = x_1^2 + x_2x_1 + x_1*/
		RandomVariableInterface aadRandomVariable05 = aadRandomVariable04.add(aadRandomVariable01);

		Map<Long, RandomVariableInterface> aadGradient = ((RandomVariableDifferentiableInterface)aadRandomVariable05).getGradient();

		/* dy/dx_1 = x_1 * 2 + x_2 + 1
		 * dy/dx_2 = x_1 */
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{
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

		RandomVariable randomVariable01 = new RandomVariable(0.0,
				new double[] {3.0, 1.0, 0.0, 2.0, 4.0});
		RandomVariable randomVariable02 = new RandomVariable(0.0,
				new double[] {-4.0, -2.0, 0.0, 2.0, 4.0} );

		/*x_1*/
		RandomVariableInterface aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/*x_2*/
		RandomVariableInterface aadRandomVariable02 = randomVariableFactory.createRandomVariable(randomVariable02.getFiltrationTime(), randomVariable02.getRealizations());

		/* x_3 = x_1 + x_2 */
		RandomVariableInterface aadRandomVariable03 = aadRandomVariable01.add(aadRandomVariable02);
		/* x_4 = x_3 * x_1 */
		RandomVariableInterface aadRandomVariable04 = aadRandomVariable03.mult(aadRandomVariable01);
		/* x_5 = x_4 + x_1 = ((x_1 + x_2) * x_1) + x_1 = x_1^2 + x_2x_1 + x_1*/
		RandomVariableInterface aadRandomVariable05 = aadRandomVariable04.add(aadRandomVariable01);

		Map<Long, RandomVariableInterface> aadGradient = ((RandomVariableDifferentiableInterface) aadRandomVariable05).getGradient();

		/* dy/dx_1 = x_1 * 2 + x_2 + 1
		 * dy/dx_2 = x_1 */
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{
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
		int lengthOfVectors = (int) Math.pow(10, 4);

		double[] x = new double[lengthOfVectors];

		for(int i=0; i < lengthOfVectors; i++){
			x[i] = Math.random();
		}

		RandomVariable randomVariable01 = new RandomVariable(0.0, x);

		/*x_1*/
		RandomVariableInterface aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);

		RandomVariableDifferentiableInterface sum = randomVariableFactory.createRandomVariable(0.0);
		for(int i = 0; i < numberOfIterations; i++){
			sum = (RandomVariableDifferentiableInterface) sum.add(aadRandomVariable01);
		}

		Map<Long, RandomVariableInterface> aadGradient = sum.getGradient();
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{new RandomVariable(numberOfIterations)};

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
			int lengthOfVectors = (int) Math.pow(10, 7);

			double[] x = new double[lengthOfVectors];
			Random random = new Random(314151);
			for(int i=0; i < lengthOfVectors; i++) {
				x[i] = random.nextDouble();
			}

			RandomVariable randomVariable01 = new RandomVariable(0.0, x);

			/*x_1*/
			RandomVariableInterface aadRandomVariable01 = randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

			/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
			int numberOfIterations =  (int) Math.pow(10, 3);

			long startValuation = System.currentTimeMillis();

			RandomVariableDifferentiableInterface sum = randomVariableFactory.createRandomVariable(0.0);
			for(int i = 0; i < numberOfIterations; i++){
				sum = (RandomVariableDifferentiableInterface) sum.add(aadRandomVariable01);
			}

			long endValuation = System.currentTimeMillis();
			long millisValuation = endValuation-startValuation;

			long startAutoDiffDerivative = System.currentTimeMillis();

			Map<Long, RandomVariableInterface> aadGradient = sum.getGradient();

			long endAutoDiffDerivative = System.currentTimeMillis();
			long millisAutoDiffDerivative = endAutoDiffDerivative-startAutoDiffDerivative;

			RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{new RandomVariable(numberOfIterations)};

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
		RandomVariableDifferentiableInterface randomVariable01 =
				randomVariableFactory.createRandomVariable(0.0, x);

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);

		RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);
		for(int i = 0; i < numberOfIterations; i++) {
			sum = sum.add(randomVariable01);
		}

		Map<Long, RandomVariableInterface> aadGradient = ((RandomVariableDifferentiableInterface) sum).getGradient();
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{new RandomVariable(numberOfIterations)};

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
		BrownianMotionInterface brownianMotion = new BrownianMotion(new TimeDiscretization(0.0, 1.0), 1 /* numberOfFactors */, numberOfPaths, seed);
		RandomVariableInterface brownianIncrement = brownianMotion.getIncrement(0, 0);

		RandomVariableDifferentiableInterface x = randomVariableFactory.createRandomVariable(1.0);

		RandomVariableInterface y = x.mult(brownianIncrement.sub(brownianIncrement.average())).average().mult(brownianIncrement);

		Map<Long, RandomVariableInterface> aadGradient = ((RandomVariableDifferentiableInterface) y).getGradient();

		RandomVariableInterface derivative = aadGradient.get(x.getID());

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

		RandomVariable randomVariable01 = new RandomVariable(0.0, x);
		RandomVariable randomVariable02 = new RandomVariable(0.0, x);

		/*x_1*/
		RandomVariableDifferentiableInterface aadRandomVariable01 =
				randomVariableFactory.createRandomVariable(randomVariable01.getFiltrationTime(), randomVariable01.getRealizations());

		/* throws StackOverflowError/OutOfMemoryError for >= 10^4 iterations */
		int numberOfIterations =  (int) Math.pow(10, 3);

		/*
		 * sum = \Sigma_{i=0}^{n-1} (x_1 + a)
		 * Note: we like to differentiate with respect to x_1, that is, a should have no effect!
		 */

		RandomVariableInterface sum = randomVariableFactory.createRandomVariable(0.0);

		for(int i = 0; i < numberOfIterations; i++){
			sum = sum.add(aadRandomVariable01);
			sum = sum.add(randomVariable02);
		}

		Map<Long, RandomVariableInterface> aadGradient = ((RandomVariableDifferentiableInterface) sum).getGradient();
		RandomVariableInterface[] analyticGradient = new RandomVariableInterface[]{new RandomVariable(numberOfIterations)};

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
		RandomVariableInterface[] randomVariables = new RandomVariableInterface[numberOfRandomVariables];

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
		Map<Long, RandomVariableInterface> gradientAutoDiff = ((RandomVariableDifferentiableInterface) testFunction(randomVariables)).getGradient();
		long endAAD = System.currentTimeMillis();

		/*
		 * Calcuate gradient using auto differentiation (factory implementation)
		 */

		// Note: copy random variable into a RandomVariable to ensure that an alternative implementation is used
		RandomVariableInterface[] randomVariablesValues = new RandomVariableInterface[randomVariables.length];
		for(int j = 0; j < numberOfRandomVariables; j++) {
			randomVariablesValues[j] = new RandomVariable(randomVariables[j]);

		}

		long startFD = System.currentTimeMillis();

		RandomVariableInterface[] gradientNumeric = new RandomVariableInterface[numberOfRandomVariables];
		for(int j = 0; j < numberOfRandomVariables; j++) {

			RandomVariableInterface[] randomVariables_p = randomVariablesValues.clone();
			RandomVariableInterface[] randomVariables_m = randomVariablesValues.clone();

			randomVariables_p[j] = randomVariablesValues[j].add(epsilon);
			randomVariables_m[j] = randomVariablesValues[j].sub(epsilon);

			/* df(x_1,...,x_n)/dx_i = (f(x_1 ,...,x_i + \epsilon,...,x_n) - f(x_1 ,...,x_i - \epsilon,...,x_n))/(2 * \epsilon) */
			gradientNumeric[j] = testFunction(randomVariables_p).sub(testFunction(randomVariables_m)).div(2*epsilon);
		}
		long endFD = System.currentTimeMillis();

		System.out.println("Time needed for AAD (" + randomVariableFactory.getClass().getSimpleName() + "): " + ((endAAD - startAAD) / 1000.0) + "s");
		System.out.println("Time needed for FD: " + ((endFD - startFD) / 1000.0) + "s");

		for(int i=0; i<gradientNumeric.length;i++) {
			RandomVariableInterface diffNumeric = gradientNumeric[i];
			RandomVariableInterface diffAutoDiff =  gradientAutoDiff.get(((RandomVariableDifferentiableInterface)randomVariables[i]).getID());
			double errorL1 = diffNumeric.sub(diffAutoDiff).abs().getAverage();
			/* if the average of the absolute error is not too big give okay*/
			Assert.assertEquals(0.0, errorL1, delta);
			//			Assert.assertEquals(0.0, gradientNumeric[i].sub(gradientAutoDiff.get(keys[i])).abs().getAverage(), delta);
		}
	}

	private RandomVariableInterface testFunction(RandomVariableInterface[] randomVariables){

		RandomVariableInterface result = randomVariables[0];
		for(int i = 1; i < randomVariables.length; i++) {
			result = result.addProduct(randomVariables[i-1].abs(), randomVariables[i].exp());
		}

		result = result.cap(randomVariables[randomVariables.length-1]).add(result.cap(randomVariables[randomVariables.length-1]));

		return result;
	}
}
