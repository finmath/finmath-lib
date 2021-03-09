/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo;

import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.DoubleUnaryOperator;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.functions.JarqueBeraTest;
import net.finmath.montecarlo.automaticdifferentiation.backward.RandomVariableDifferentiableAADFactory;
import net.finmath.montecarlo.automaticdifferentiation.forward.RandomVariableDifferentiableADFactory;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class BrownianMotionTest {

	static final DecimalFormat formatterReal2	= new DecimalFormat(" 0.00");
	static final DecimalFormat formatterSci4	= new DecimalFormat(" 0.0000E00;-0.0000E00");
	static final DecimalFormat formatterSci1	= new DecimalFormat(" 0E00;-0.E00");

	private final RandomVariableFactory randomVariableFactory;

	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ new RandomVariableFromArrayFactory(true /* isUseDoublePrecisionFloatingPointImplementation */) },
			{ new RandomVariableFromArrayFactory(false /* isUseDoublePrecisionFloatingPointImplementation */) },
			{ new RandomVariableDifferentiableAADFactory() },
			{ new RandomVariableDifferentiableADFactory() },
		});
	}

	public BrownianMotionTest(final RandomVariableFactory randomVariableFactory) {
		super();
		this.randomVariableFactory = randomVariableFactory;
	}

	/**
	 * This test compares the numerical sampled density (from a histogram) of the Brownian motion W(T)
	 * with the analytic density.
	 */
	@Test
	public void testDensity() {
		final int seed = 3141;
		final int numberOfFactors = 1;
		final int numberOfPaths = 10000000;
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0, 10, 1.0);
		final BrownianMotion brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, numberOfFactors, numberOfPaths, seed, randomVariableFactory);

		RandomVariable brownianMotionAtTime = brownianMotion.getBrownianIncrement(0, 0);
		for(int timeIndex=1; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final double[] intervalPoints = (new TimeDiscretizationFromArray(-2, 101, 4.0/100)).getAsDoubleArray();
			final double[] histOfNormalFromBM = brownianMotionAtTime.getHistogram(intervalPoints);

			final double time = brownianMotionAtTime.getFiltrationTime();
			final DoubleUnaryOperator densityAnalytic = new DoubleUnaryOperator() {
				@Override
				public double applyAsDouble(final double x) { return Math.exp(-x*x/2.0/time) / Math.sqrt(2 * Math.PI * time); }
			};

			for(int i=0; i<intervalPoints.length-1; i++) {
				final double center = (intervalPoints[i+1]+intervalPoints[i])/2.0;
				final double size = intervalPoints[i+1]-intervalPoints[i];

				final double density = histOfNormalFromBM[i+1] / size;
				final double densityAnalyt = densityAnalytic.applyAsDouble(center);

				Assert.assertEquals("Density", densityAnalyt, density, 5E-3);
			}
			brownianMotionAtTime = brownianMotionAtTime.add(brownianMotion.getBrownianIncrement(timeIndex, 0));
		}
	}

	@Test
	public void testScalarValuedBrownianMotionTerminalDistribution() {
		// The parameters
		final int		seed		= 53252;
		final double	lastTime	= 1;//0.001;
		final double	dt			= 1;//0.001;

		System.out.println("Test of mean and variance of a single Brownian increment.");

		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)(lastTime/dt), dt);

		for(int numberOfPaths = 1000; numberOfPaths <= 100000000; numberOfPaths *= 10) {

			// Test the quality of the Brownian motion
			final BrownianMotion brownian = new BrownianMotionFromMersenneRandomNumbers(
					timeDiscretization,
					1,
					numberOfPaths,
					seed,
					randomVariableFactory
					);

			System.out.print("\tNumber of path = " + formatterSci1.format(numberOfPaths) + "\t ");

			final RandomVariable brownianRealization = brownian.getBrownianIncrement(0, 0);

			final double mean		= brownianRealization.getAverage();
			final double variance	= brownianRealization.getVariance();

			System.out.print("error of mean = " + formatterSci4.format(mean) + "\t error of variance = " + formatterSci4.format(variance-dt));

			Assert.assertTrue(Math.abs(mean         ) < 3.0 * Math.pow(dt,0.5) / Math.pow(numberOfPaths,0.5));
			Assert.assertTrue(Math.abs(variance - dt) < 3.0 * Math.pow(dt,1.0) / Math.pow(numberOfPaths,0.5));

			System.out.println(" - OK");
		}

		System.out.println();
	}

	@Test
	public void testScalarValuedBrownianMotionWithJarqueBeraTest() {
		// The parameters
		final int		numberOfPaths	= 100000;
		final int		seed		= 31415;
		final double	lastTime	= 60;
		final double	dt			= 0.25;

		System.out.println("Jarque-Bera test of subsequent Brownian increments.");

		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)(lastTime/dt), dt);

		// Test the quality of the Brownian motion
		final BrownianMotion brownian = new BrownianMotionFromMersenneRandomNumbers(
				timeDiscretization,
				1,
				numberOfPaths,
				seed,
				randomVariableFactory
				);

		final JarqueBeraTest jb = new JarqueBeraTest();
		int fail = 0;
		for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final RandomVariable brownianRealization = brownian.getBrownianIncrement(timeIndex, 0);
			final double test = jb.test(brownianRealization);

			System.out.print(timeIndex + ":\t" + test);
			if(test > 4.6) {
				fail++;
				System.out.println(" - fail");
			}
			else {
				System.out.println(" - OK");
			}
		}

		System.out.println(fail + " out of " + timeDiscretization.getNumberOfTimeSteps() + " failed.");

		Assert.assertTrue("Test on normal distribution.", 10.0 * fail < timeDiscretization.getNumberOfTimeSteps());

		System.out.println();
	}

	@Test
	public void testBrownianIncrementSquaredDrift() {
		// The parameters
		final int numberOfPaths	= 10000;
		final int seed			= 53252;

		final double lastTime = 4.0;
		final double dt = 0.001;

		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)(lastTime/dt), dt);

		// Test the quality of the Brownian motion
		final BrownianMotion brownian = new BrownianMotionFromMersenneRandomNumbers(
				timeDiscretization,
				2,
				numberOfPaths,
				seed,
				randomVariableFactory
				);

		System.out.println("Test of average and variance of the integral of (Delta W)^2.");
		System.out.println("Time step size: " + dt + "  Number of path: " + numberOfPaths);

		RandomVariable sumOfSquaredIncrements 	= brownian.getRandomVariableForConstant(0.0);
		RandomVariable sumOfCrossIncrements	= brownian.getRandomVariableForConstant(0.0);
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			final RandomVariable brownianIncrement1 = brownian.getBrownianIncrement(timeIndex,0);
			final RandomVariable brownianIncrement2 = brownian.getBrownianIncrement(timeIndex,1);

			// Calculate x = \int dW1(t) * dW1(t)
			final RandomVariable squaredIncrements = brownianIncrement1.squared();
			sumOfSquaredIncrements = sumOfSquaredIncrements.add(squaredIncrements);

			// Calculate x = \int dW1(t) * dW2(t)
			final RandomVariable covarianceIncrements = brownianIncrement1.mult(brownianIncrement2);
			sumOfCrossIncrements = sumOfCrossIncrements.add(covarianceIncrements);
		}

		final double time								= timeDiscretization.getTime(timeDiscretization.getNumberOfTimeSteps());
		final double meanOfSumOfSquaredIncrements		= sumOfSquaredIncrements.getAverage();
		final double varianceOfSumOfSquaredIncrements	= sumOfSquaredIncrements.getVariance();
		final double meanOfSumOfCrossIncrements		= sumOfCrossIncrements.getAverage();
		final double varianceOfSumOfCrossIncrements	= sumOfCrossIncrements.getVariance();

		Assert.assertTrue(Math.abs(meanOfSumOfSquaredIncrements-time) < 1.0E-3);
		Assert.assertTrue(Math.abs(varianceOfSumOfSquaredIncrements) < 1.0E-2);
		Assert.assertTrue(Math.abs(meanOfSumOfCrossIncrements) < 1.0E-3);
		Assert.assertTrue(Math.abs(varianceOfSumOfCrossIncrements) < 1.0E-2);


		System.out.println("\t              t = " + formatterReal2.format(time));
		System.out.println("\tint_0^t dW1 dW1 = " + formatterSci4.format(meanOfSumOfSquaredIncrements)
		+ "\t (Monte-Carlo variance: " + formatterSci4.format(varianceOfSumOfSquaredIncrements) + ")");
		System.out.println("\tint_0^t dW1 dW2 = " + formatterSci4.format(meanOfSumOfCrossIncrements)
		+ "\t (Monte-Carlo variance: " + formatterSci4.format(varianceOfSumOfCrossIncrements) + ")");

		System.out.println();
	}

	@Test
	public void testSerialization() {
		// The parameters
		final int numberOfPaths	= 10000;
		final int seed			= 53252;

		final double lastTime = 2.0;
		final double dt = 0.1;

		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)(lastTime/dt), dt);

		// Test the quality of the Brownian motion
		final BrownianMotion brownian = new BrownianMotionFromMersenneRandomNumbers(
				timeDiscretization,
				2,
				numberOfPaths,
				seed,
				randomVariableFactory
				);

		final RandomVariable value = brownian.getBrownianIncrement(10, 0);

		/*
		 * Serialize to a byte stream
		 */
		byte[] serializedObject = null;
		try {
			final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			final ObjectOutputStream oos = new ObjectOutputStream( baos );
			oos.writeObject(brownian);
			serializedObject = baos.toByteArray();
		} catch (final IOException e) {
			fail("Serialization failed with exception " + e.getMessage());
		}

		/*
		 * De-serialize from a byte stream
		 */
		BrownianMotion brownianClone = null;
		try {
			final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(serializedObject) );
			brownianClone = (BrownianMotion)ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			fail("Deserialization failed with exception " + e.getMessage());
		}

		final RandomVariable valueClone = brownianClone.getBrownianIncrement(10, 0);

		Assert.assertNotSame("Comparing random variable from original and deserialized object: different references.", value, valueClone);
		Assert.assertTrue("Comparing random variable from original and deserialized object: equals().", value.equals(valueClone));
	}

}
