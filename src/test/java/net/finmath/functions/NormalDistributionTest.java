/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
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
			double cdfUp = NormalDistribution.cumulativeDistribution(x+epsilon);
			double cdfDown = NormalDistribution.cumulativeDistribution(x-epsilon);

			double densityNumerical = (cdfUp-cdfDown) / (2 * epsilon);
			double densityAnalytic = Math.exp(-x*x/2.0) / Math.sqrt(2 * Math.PI);

			double error = densityNumerical-densityAnalytic;
			System.out.println(error);

			Assert.assertEquals("Numerical differentiation of CDF", densityAnalytic, densityNumerical, 1E-10);
		}
	}

	@Test
	public void testInverseCumulativeDistribution() {

		final double epsilon = 1E-10;

		for(double y=NormalDistribution.cumulativeDistribution(-4.0); y<= NormalDistribution.cumulativeDistribution(4.0); y += 0.001) {
			double icdfUp = NormalDistribution.inverseCumulativeDistribution(y+epsilon);
			double icdfDown = NormalDistribution.inverseCumulativeDistribution(y-epsilon);

			double x = NormalDistribution.inverseCumulativeDistribution(y);
			double derivativeNumerical = (icdfUp-icdfDown) / (2 * epsilon);
			double derivativeAnalytic = 1.0 / (Math.exp(-x*x/2.0) / Math.sqrt(2 * Math.PI));

			double error = derivativeNumerical-derivativeAnalytic;
			System.out.println(y + "\t" + derivativeNumerical + "\t" + derivativeAnalytic + "\t" + error);

			Assert.assertEquals("Numerical differentiation of ICDF", derivativeAnalytic, derivativeNumerical, 5E-5);
		}
	}
}
