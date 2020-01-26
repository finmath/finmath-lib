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
		final RandomNumberGenerator uniformRandomNumberGenerator2D = new HaltonSequence(new int[] { 2,3 });
		final DoubleUnaryOperator targetDensity = new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) { return Math.sqrt(2.0 / Math.PI) * Math.exp(- x*x / 2.0); }
		};
		final DoubleUnaryOperator referenceDensity = new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) { return Math.exp(- x); }
		};
		final DoubleUnaryOperator referenceDistributionICDF = new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) { return -Math.log(1 - x); }
		};
		final double acceptanceLevel = Math.sqrt(2.0 / Math.PI * Math.exp(1));
		final RandomNumberGenerator normalRandomNumberGeneratorAR = new AcceptanceRejectionRandomNumberGenerator(uniformRandomNumberGenerator2D, targetDensity, referenceDensity, referenceDistributionICDF, acceptanceLevel);

		final RandomNumberGenerator uniformRandomNumberGenerator1D = new HaltonSequence(new int[] { 2 });

		final Random randomForSign = new Random(3141);

		final int numberOfPaths = 1000000;
		final double[] normalFromAR = new double[numberOfPaths];
		final double[] normalFromICDF = new double[numberOfPaths];
		for(int i = 0; i<numberOfPaths; i++) {
			final double normalPositive = normalRandomNumberGeneratorAR.getNext()[0];

			normalFromAR[i] = (randomForSign.nextDouble() > 0.5 ? 1.0 : -1.0) * normalPositive;

			normalFromICDF[i] = NormalDistribution.inverseCumulativeDistribution(uniformRandomNumberGenerator1D.getNext()[0]);
		}

		final double[] interv = (new TimeDiscretizationFromArray(-3, 101, 6.0/100)).getAsDoubleArray();
		final double[] histOfNormalFromAR = new RandomVariableFromDoubleArray(0.0, normalFromAR).getHistogram(interv);
		final double[] histOfNormalFromICDF = new RandomVariableFromDoubleArray(0.0, normalFromICDF).getHistogram(interv);

		// Build and check density
		for(int i=0; i<interv.length-1; i++) {
			final double x = (interv[i]+interv[i+1])/2.0;
			final double densityAR = histOfNormalFromAR[i+1] / (interv[i+1]-interv[i]);
			final double densityICDF = histOfNormalFromICDF[i+1] / (interv[i+1]-interv[i]);

			System.out.println(x + "\t" + densityAR  + "\t" + densityICDF + "\t" + (densityAR-densityICDF));

			Assert.assertEquals("Density", densityICDF, densityAR, 1E-2);
		}
	}
}
