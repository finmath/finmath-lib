/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2015
 */

package net.finmath.integration;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.interpolation.RationalFunctionInterpolation;
import net.finmath.interpolation.RationalFunctionInterpolation.ExtrapolationMethod;
import net.finmath.interpolation.RationalFunctionInterpolation.InterpolationMethod;

/**
 * @author Christian Fries
 *
 */
public class TrapezoidalRealIntegratorTest {

	@Test
	public void test() {
		final double[] points = { 0.0, 0.1, 0.5, 1.0, 2.0, 5.0, 10.0 };
		final double[] values = { 1.0, 2.1, 3.5, 1.0, 4.0, 1.0, 7.0 };

		final RationalFunctionInterpolation interpolation = new RationalFunctionInterpolation(points, values, InterpolationMethod.LINEAR, ExtrapolationMethod.CONSTANT);

		final double[] evaluationPoints = points;

		final AbstractRealIntegral trapezoidalRealIntegrator = new TrapezoidalRealIntegrator(0.75, 3.5, evaluationPoints);
		final AbstractRealIntegral simpsonRealIntegrator = new SimpsonRealIntegrator(0.75, 3.5, 2000);

		/*
		 * Trapezoidal integration of the piece-wise linear function is exact, if we use points as integration points.
		 * Simpson's rule is using an equi-distant discretization and is not exact.
		 */
		final double valueTrapezoidal = trapezoidalRealIntegrator.integrate(interpolation);
		final double valueSimpsons = simpsonRealIntegrator.integrate(interpolation);

		Assert.assertEquals("Difference of trapezoidal and Simpson's rule", 0.0, valueSimpsons-valueTrapezoidal, 0.000001);
	}
}
