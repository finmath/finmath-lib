/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.tests.montecarlo;

import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;

import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

import org.junit.Test;

/**
 * @author Christian Fries
 * 
 */
public class BrownianMotionTests {

	static final DecimalFormat fromatterReal2	= new DecimalFormat(" 0.00");
	static final DecimalFormat fromatterSci4	= new DecimalFormat(" 0.0000E00;-0.0000E00");
	
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
		
		System.out.println("Average and variance of the integral of (Delta W)^2.\nTime step size: " + dt + "  Number of path: " + numberOfPaths + "\n");

		RandomVariableInterface sumOfSquaredIncrements 	= new RandomVariable(0.0);
		RandomVariableInterface sumOfCrossIncrements		= new RandomVariable(0.0);
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

		assertTrue(Math.abs(meanOfSumOfSquaredIncrements-time) < 1.0E-3);
		assertTrue(Math.abs(varianceOfSumOfSquaredIncrements) < 1.0E-2);
		assertTrue(Math.abs(meanOfSumOfCrossIncrements) < 1.0E-3);
		assertTrue(Math.abs(varianceOfSumOfCrossIncrements) < 1.0E-2);

		
		System.out.println("              t = " + fromatterReal2.format(time));
		System.out.println("int_0^t dW1 dW1 = " + fromatterSci4.format(meanOfSumOfSquaredIncrements)
				+ "\t (Monte-Carlo variance: " + fromatterSci4.format(varianceOfSumOfSquaredIncrements) + ")");
		System.out.println("int_0^t dW1 dW2 = " + fromatterSci4.format(meanOfSumOfCrossIncrements)
				+ "\t (Monte-Carlo variance: " + fromatterSci4.format(varianceOfSumOfCrossIncrements) + ")");
	}
}
