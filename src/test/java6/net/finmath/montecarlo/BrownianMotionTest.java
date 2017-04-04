/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.montecarlo;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.functions.JarqueBeraTest;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 * 
 */
public class BrownianMotionTest {

	static final DecimalFormat formatterReal2	= new DecimalFormat(" 0.00");
	static final DecimalFormat formatterSci4	= new DecimalFormat(" 0.0000E00;-0.0000E00");
	static final DecimalFormat formatterSci1	= new DecimalFormat(" 0E00;-0.E00");

	@Test
	public void testScalarValuedBrownianMotionTerminalDistribution() {
		// The parameters
		int		seed		= 53252;
		double	lastTime	= 1;//0.001;
		double	dt			= 1;//0.001;

		System.out.println("Test of mean and variance of a single Brownian increment.");

		// Create the time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0, (int)(lastTime/dt), dt);

		for(int numberOfPaths = 1000; numberOfPaths <= 100000000; numberOfPaths *= 10) {

			// Test the quality of the Brownian motion
			BrownianMotion brownian = new BrownianMotion(
					timeDiscretization,
					1,
					numberOfPaths,
					seed
			);
			
			System.out.print("\tNumber of path = " + formatterSci1.format(numberOfPaths) + "\t ");

			RandomVariableInterface brownianRealization = brownian.getBrownianIncrement(0, 0);
			
			double mean		= brownianRealization.getAverage();
			double variance	= brownianRealization.getVariance();
			
			System.out.print("error of mean = " + formatterSci4.format(mean) + "\t error of variance = " + formatterSci4.format(variance-dt));

			Assert.assertTrue(Math.abs(mean         ) < 3.0 * Math.pow(dt,0.5) / Math.pow(numberOfPaths,0.5));
			Assert.assertTrue(Math.abs(variance - dt) < 3.0 * Math.pow(dt,1.0) / Math.pow(numberOfPaths,0.5));
			
			System.out.println(" - OK");
		}

		System.out.println("");
	}
	
	@Test
	public void testScalarValuedBrownianMotionWithJarqueBeraTest() {
		// The parameters
		int		numberOfPaths	= 100000;
		int		seed		= 31415;
		double	lastTime	= 60;
		double	dt			= 0.25;

		System.out.println("Jarque-Bera test of subsequent Brownian increments.");

		// Create the time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0, (int)(lastTime/dt), dt);

		// Test the quality of the Brownian motion
		BrownianMotion brownian = new BrownianMotion(
				timeDiscretization,
				1,
				numberOfPaths,
				seed
		);
		
		JarqueBeraTest jb = new JarqueBeraTest();
		int fail = 0;
		for(int timeIndex = 0; timeIndex < timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			RandomVariableInterface brownianRealization = brownian.getBrownianIncrement(timeIndex, 0);
			double test = jb.test(brownianRealization);

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

		System.out.println("");
	}

	@Test
	public void testBrownianIncrementSquaredDrift() {
		// The parameters
		int numberOfPaths	= 10000;
		int seed			= 53252;
		
		double lastTime = 4.0;
		double dt = 0.001;

		// Create the time discretization
		TimeDiscretizationInterface timeDiscretization = new TimeDiscretization(0.0, (int)(lastTime/dt), dt);

		// Test the quality of the Brownian motion
		BrownianMotion brownian = new BrownianMotion(
				timeDiscretization,
				2,
				numberOfPaths,
				seed
		);
		
		System.out.println("Test of average and variance of the integral of (Delta W)^2.");
		System.out.println("Time step size: " + dt + "  Number of path: " + numberOfPaths);

		RandomVariableInterface sumOfSquaredIncrements 	= brownian.getRandomVariableForConstant(0.0);
		RandomVariableInterface sumOfCrossIncrements	= brownian.getRandomVariableForConstant(0.0);
		for(int timeIndex=0; timeIndex<timeDiscretization.getNumberOfTimeSteps(); timeIndex++) {
			RandomVariableInterface brownianIncrement1 = brownian.getBrownianIncrement(timeIndex,0);
			RandomVariableInterface brownianIncrement2 = brownian.getBrownianIncrement(timeIndex,1);

			// Calculate x = \int dW1(t) * dW1(t)
			RandomVariableInterface squaredIncrements = brownianIncrement1.squared();
			sumOfSquaredIncrements = sumOfSquaredIncrements.add(squaredIncrements);

			// Calculate x = \int dW1(t) * dW2(t)
			RandomVariableInterface covarianceIncrements = brownianIncrement1.mult(brownianIncrement2);
			sumOfCrossIncrements = sumOfCrossIncrements.add(covarianceIncrements);
		}

		double time								= timeDiscretization.getTime(timeDiscretization.getNumberOfTimeSteps());
		double meanOfSumOfSquaredIncrements		= sumOfSquaredIncrements.getAverage();
		double varianceOfSumOfSquaredIncrements	= sumOfSquaredIncrements.getVariance();
		double meanOfSumOfCrossIncrements		= sumOfCrossIncrements.getAverage();
		double varianceOfSumOfCrossIncrements	= sumOfCrossIncrements.getVariance();

		Assert.assertTrue(Math.abs(meanOfSumOfSquaredIncrements-time) < 1.0E-3);
		Assert.assertTrue(Math.abs(varianceOfSumOfSquaredIncrements) < 1.0E-2);
		Assert.assertTrue(Math.abs(meanOfSumOfCrossIncrements) < 1.0E-3);
		Assert.assertTrue(Math.abs(varianceOfSumOfCrossIncrements) < 1.0E-2);

		
		System.out.println("\t              t = " + formatterReal2.format(time));
		System.out.println("\tint_0^t dW1 dW1 = " + formatterSci4.format(meanOfSumOfSquaredIncrements)
				+ "\t (Monte-Carlo variance: " + formatterSci4.format(varianceOfSumOfSquaredIncrements) + ")");
		System.out.println("\tint_0^t dW1 dW2 = " + formatterSci4.format(meanOfSumOfCrossIncrements)
				+ "\t (Monte-Carlo variance: " + formatterSci4.format(varianceOfSumOfCrossIncrements) + ")");

		System.out.println("");
	}
}
