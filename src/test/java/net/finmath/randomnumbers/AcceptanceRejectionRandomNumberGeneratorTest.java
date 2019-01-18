/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 21 May 2018
 */
package net.finmath.randomnumbers;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.functions.NormalDistribution;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.time.TimeDiscretizationFromArray;

public class AcceptanceRejectionRandomNumberGeneratorTest {

	@Test
	public void test() {
		RandomNumberGenerator uniformRandomNumberGenerator2D = new HaltonSequence(new int[] { 2,3 });
		DoubleUnaryOperator targetDensity = x -> { return Math.sqrt(2.0 / Math.PI) * Math.exp(- x*x / 2.0); };
		DoubleUnaryOperator referenceDensity = x -> { return Math.exp(- x); };
		DoubleUnaryOperator referenceDistributionICDF = x -> { return -Math.log(1 - x); };
		double acceptanceLevel = Math.sqrt(2.0 / Math.PI * Math.exp(1));
		RandomNumberGenerator normalRandomNumberGeneratorAR = new AcceptanceRejectionRandomNumberGenerator(uniformRandomNumberGenerator2D, targetDensity, referenceDensity, referenceDistributionICDF, acceptanceLevel);

		RandomNumberGenerator uniformRandomNumberGenerator1D = new HaltonSequence(new int[] { 2 });

		Random randomForSign = new Random(3141);

		int numberOfPaths = 1000000;
		double[] normalFromAR = new double[numberOfPaths];
		double[] normalFromICDF = new double[numberOfPaths];
		for(int i = 0; i<numberOfPaths; i++) {
			double normalPositive = normalRandomNumberGeneratorAR.getNext()[0];

			normalFromAR[i] = (randomForSign.nextDouble() > 0.5 ? 1.0 : -1.0) * normalPositive;

			normalFromICDF[i] = NormalDistribution.inverseCumulativeDistribution(uniformRandomNumberGenerator1D.getNext()[0]);
		}

		double[] interv = (new TimeDiscretizationFromArray(-3, 101, 6.0/100)).getAsDoubleArray();
		double[] histOfNormalFromAR = new RandomVariableFromDoubleArray(0.0, normalFromAR).getHistogram(interv);
		double[] histOfNormalFromICDF = new RandomVariableFromDoubleArray(0.0, normalFromICDF).getHistogram(interv);

		// Build and check density
		for(int i=0; i<interv.length-1; i++) {
			double x = (interv[i]+interv[i+1])/2.0;
			double densityAR = histOfNormalFromAR[i+1] / (interv[i+1]-interv[i]);
			double densityICDF = histOfNormalFromICDF[i+1] / (interv[i+1]-interv[i]);

			System.out.println(x + "\t" + densityAR  + "\t" + densityICDF + "\t" + (densityAR-densityICDF));

			Assert.assertEquals("Density", densityICDF, densityAR, 1E-2);
		}
	}
}
