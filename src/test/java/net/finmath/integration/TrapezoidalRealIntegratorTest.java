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
