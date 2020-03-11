/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 25.03.2014
 */

package net.finmath.integration;

import java.util.function.DoubleUnaryOperator;

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

	@Before
	public void setUp() {
		final double	lowerBound = 1.0;
		final double	upperBound = 5.0;
		final int		numberOfEvaluationPoints = 100;

		integral = new SimpsonRealIntegrator(lowerBound, upperBound, numberOfEvaluationPoints, true);
	}

	@Test
	public void testCos() {

		final DoubleUnaryOperator integrand			= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return Math.cos(x);
			}
		};
		final DoubleUnaryOperator integralAnalytic	= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return Math.sin(x);
			}
		};

		final double value = integral.integrate(integrand);

		final double valueAnalytic = integralAnalytic.applyAsDouble(integral.getUpperBound())-integralAnalytic.applyAsDouble(integral.getLowerBound());

		final double error = value-valueAnalytic;

		System.out.println("Result: " + value + ". \tError: " + error);

		Assert.assertEquals("Integreation error.", 0.0, error, 1E-7);
	}

	@Test
	public void testCubic() {

		final DoubleUnaryOperator integrand			= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return 2 * x * x - x;
			}
		};
		final DoubleUnaryOperator integralAnalytic	= new DoubleUnaryOperator() {
			@Override
			public double applyAsDouble(final double x) {
				return 2 * x * x * x / 3 - x * x / 2;
			}
		};

		final double value = integral.integrate(integrand);

		final double valueAnalytic = integralAnalytic.applyAsDouble(integral.getUpperBound())-integralAnalytic.applyAsDouble(integral.getLowerBound());

		final double error = value-valueAnalytic;

		System.out.println("Result: " + value + ". \tError: " + error);

		Assert.assertEquals("Integreation error.", 0.0, error, 1E-13);
	}
}
