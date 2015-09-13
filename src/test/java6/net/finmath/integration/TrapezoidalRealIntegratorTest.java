/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 06.09.2015
 */

package net.finmath.integration;

import net.finmath.compatibility.java.util.function.DoubleUnaryOperator;
import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Christian Fries
 *
 */
public class TrapezoidalRealIntegratorTest {

	private AbstractRealIntegral integral;

	@Before
	public void setUp() {
		final double	lowerBound = 1.0;
		final double	upperBound = 5.0;
		final int		numberOfEvaluationPoints = 10000;

		integral = new TrapezoidalRealIntegrator(lowerBound, upperBound, numberOfEvaluationPoints);
	}

	@Test
	public void testCos() {

		DoubleUnaryOperator integrand			= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(double x) {
				return Math.cos(x);
			}
		};

		DoubleUnaryOperator integralAnalytic	= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(double x) {
				return Math.sin(x);
			}
		};

		double value = integral.integrate(integrand);

		double valueAnalytic = integralAnalytic.applyAsDouble(integral.getUpperBound())-integralAnalytic.applyAsDouble(integral.getLowerBound());

		double error = value-valueAnalytic;

		System.out.println("Result: " + value + ". \tError: " + error);

		Assert.assertEquals("Integreation error.", 0.0, error, 1E-7);
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

		Assert.assertEquals("Integreation error.", 0.0, error, 2.5E-7);
	}

	@Test
	public void testWithInterpolationFunction() {
		double[] points = { 0.0, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0 };
		double[] values = { 1.0, 2.1, 3.5, 1.0, 4.0, 1.0, 7.0 };

		RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(points, values, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT);

		double[] evaluationPoints = points;

		AbstractRealIntegral trapezoidalRealIntegrator = new TrapezoidalRealIntegrator(0.75, 3.5, evaluationPoints);
		AbstractRealIntegral simpsonRealIntegrator = new SimpsonRealIntegrator(0.75, 3.5, 2000);

		/*
		 * Trapezoidal integration of the piece-wise linear function is exact, if we use points as integration points.
		 * Simpson's rule is using an equi-distant discretization and is not exact.
		 */
		double valueTrapezoidal = trapezoidalRealIntegrator.integrate(interpolation);
		double valueSimpsons = simpsonRealIntegrator.integrate(interpolation);

		Assert.assertEquals("Difference of trapezoidal and Simpson's rule", 0.0, valueSimpsons-valueTrapezoidal, 0.000001);
	}
}
