/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 25.03.2014
 */

package net.finmath.integration;

import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for the SimpsonRealIntegrator.
 * 
 * @author Christian Fries
 */
public class SimpsonRealIntegratorTest {

	private AbstractRealIntegral integral;
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		final double	lowerBound = 1.0;
		final double	upperBound = 5.0;
		final int		numberOfEvaluationPoints = 100;

		integral = new SimpsonRealIntegrator(lowerBound, upperBound, numberOfEvaluationPoints, true);
	}

	@Test
	public void testCos() {
		
		DoubleUnaryOperator integrand			= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(double x) {
				return Math.cos(x);
			}};
		DoubleUnaryOperator integralAnalytic	= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(double x) {
				return Math.sin(x);
			}};
		
		double value = integral.integrate(integrand);
		
		double valueAnalytic = integralAnalytic.applyAsDouble(integral.getUpperBound())-integralAnalytic.applyAsDouble(integral.getLowerBound());
		
		double error = value-valueAnalytic;

		System.out.println("Result: " + value + ". \tError: " + error);
		
		Assert.assertTrue(Math.abs(error) < 1E-7);
	}

	@Test
	public void testCubic() {

		DoubleUnaryOperator integrand			= new DoubleUnaryOperator() {
			public double applyAsDouble(double x) {
				return 2 * x * x - x;
			}
		};
		DoubleUnaryOperator integralAnalytic	= new DoubleUnaryOperator() {
			public double applyAsDouble(double x) {
				return 2 * x * x * x / 3 - x * x / 2;
			}
		};
		
		double value = integral.integrate(integrand);
		
		double valueAnalytic = integralAnalytic.applyAsDouble(integral.getUpperBound())-integralAnalytic.applyAsDouble(integral.getLowerBound());
		
		double error = value-valueAnalytic;

		System.out.println("Result: " + value + ". \tError: " + error);
		
		Assert.assertTrue(Math.abs(error) < 1E-13);
	}
}
