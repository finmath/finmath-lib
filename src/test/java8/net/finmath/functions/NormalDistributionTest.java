/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 13.07.2018
 */

package net.finmath.functions;

import org.junit.Assert;
import org.junit.Test;

/**
 * Simple unit test for the normal distribution.
 *
 * @author Christian Fries
 */
public class NormalDistributionTest {

	@Test
	public void testCumulativeDistribution() {

		final double epsilon = 1E-5;

		for(double x=-4.0; x<= 4.0; x += 0.01) {
			final double cdfUp = NormalDistribution.cumulativeDistribution(x+epsilon);
			final double cdfDown = NormalDistribution.cumulativeDistribution(x-epsilon);

			final double densityNumerical = (cdfUp-cdfDown) / (2 * epsilon);
			final double densityAnalytic = Math.exp(-x*x/2.0) / Math.sqrt(2 * Math.PI);

			final double error = densityNumerical-densityAnalytic;
			System.out.println(error);

			Assert.assertEquals("Numerical differentiation of CDF", densityAnalytic, densityNumerical, 1E-10);
		}
	}

	@Test
	public void testInverseCumulativeDistribution() {

		final double epsilon = 1E-10;

		for(double y=NormalDistribution.cumulativeDistribution(-4.0); y<= NormalDistribution.cumulativeDistribution(4.0); y += 0.001) {
			final double icdfUp = NormalDistribution.inverseCumulativeDistribution(y+epsilon);
			final double icdfDown = NormalDistribution.inverseCumulativeDistribution(y-epsilon);

			final double x = NormalDistribution.inverseCumulativeDistribution(y);
			final double derivativeNumerical = (icdfUp-icdfDown) / (2 * epsilon);
			final double derivativeAnalytic = 1.0 / (Math.exp(-x*x/2.0) / Math.sqrt(2 * Math.PI));

			final double error = derivativeNumerical-derivativeAnalytic;
			System.out.println(y + "\t" + derivativeNumerical + "\t" + derivativeAnalytic + "\t" + error);

			Assert.assertEquals("Numerical differentiation of ICDF", derivativeAnalytic, derivativeNumerical, 5E-5);
		}
	}
}
