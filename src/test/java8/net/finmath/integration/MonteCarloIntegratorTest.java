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
 * Unit test for the MonteCarloIntegrator.
 *
 * @author Christian Fries
 */
public class MonteCarloIntegratorTest {

	private final double	lowerBound = 1.0;
	private final double	upperBound = 5.0;
	private final int		numberOfEvaluationPoints = 1000000;
	private final int		seed = 3141;

	private AbstractRealIntegral integral;

	@Before
	public void setUp() {
		integral = new MonteCarloIntegrator(lowerBound, upperBound, numberOfEvaluationPoints, seed, true);
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

		final double q = 0.05;
		final double variance = integral.integrate(x -> Math.pow(integrand.applyAsDouble(x)-value, 2));
		final double monteCarloError = 1.0/Math.sqrt(q) * Math.sqrt(variance/((MonteCarloIntegrator)integral).getNumberOfEvaluationPoints());

		final double optimalRate = 2.0 / numberOfEvaluationPoints;

		System.out.println("Result: " + value + ". \tError: " + error + ". \tMC: " + monteCarloError + ". \tBest accuracy: " + optimalRate);

		Assert.assertEquals("Integreation error.", 0.0, error, monteCarloError);
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

		final double q = 0.05;
		final double variance = integral.integrate(x -> Math.pow(integrand.applyAsDouble(x)-value, 2));
		final double monteCarloError = 1.0/Math.sqrt(q) * Math.sqrt(variance/((MonteCarloIntegrator)integral).getNumberOfEvaluationPoints());

		System.out.println("Result: " + value + ". \tError: " + error + ". \tMC: " + monteCarloError);

		Assert.assertEquals("Integreation error.", 0.0, error, monteCarloError);
	}
}
