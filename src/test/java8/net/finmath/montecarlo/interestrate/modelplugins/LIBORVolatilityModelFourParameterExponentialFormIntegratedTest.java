/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.06.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.Random;
import java.util.function.DoubleUnaryOperator;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.integration.SimpsonRealIntegrator;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModel;
import net.finmath.montecarlo.interestrate.models.covariance.LIBORVolatilityModelFourParameterExponentialFormIntegrated;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Unit test for <code>LIBORVolatilityModelFourParameterExponentialFormIntegrated</code>.
 *
 * This test shows that the analytic formula used in <code>LIBORVolatilityModelFourParameterExponentialFormIntegrated</code>
 * agrees with the result a numerical integration obtained from <code>LIBORVolatilityModelFourParameterExponentialForm</code>
 * given that its time discretization step size is small enough.
 * @author Christian Fries
 */
public class LIBORVolatilityModelFourParameterExponentialFormIntegratedTest {

	@Test
	public void test() {
		/*
		 * Generate a set of test parameters within a given range
		 */
		final Random random = new Random(3141);

		final double aMin = 0.0;
		final double aMax = 1.0;
		final double bMin = 0.0;
		final double bMax = 2.0;
		final double cMin = -0.001;
		final double cMax = 0.05;
		final double dMin = 0.0;
		final double dMax = 0.2;

		/*
		 * Difficult values are small values for c, e.g.
		 * c =  1.6442404690564238E-6;
		 * c =  7.1699989962897840E-6;
		 * c = -8.5350795667182840E-8;
		 * c = -3.3771868628101887E-5;
		 */
		double error = 0.0;
		for(int i=0; i<1000; i++) {
			final double a = aMin + random.nextDouble() * (aMax-aMin);
			final double b = bMin + random.nextDouble() * (bMax-bMin);
			final double c = cMin + random.nextDouble() * (cMax-cMin);
			final double d = dMin + random.nextDouble() * (dMax-dMin);

			final int numberOfTimePoints = 100;
			final TimeDiscretization td = new TimeDiscretizationFromArray(0.0, numberOfTimePoints, 50.0/numberOfTimePoints);
			final LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(td, td, a, b, c, d, false);

			final int timeIndex = random.nextInt(numberOfTimePoints-1);
			final int liborIndex = random.nextInt(numberOfTimePoints-timeIndex-1)+1;

			final double volatilityAnalytical = volatilityModel.getVolatility(timeIndex, timeIndex+liborIndex).getAverage();

			/*
			 * Numerical integration
			 */
			final int numberOfEvaluationPoints = 100000;
			final double t1 = td.getTime(timeIndex);
			final double t2 = td.getTime(timeIndex+1);
			final double maturity = td.getTime(timeIndex+liborIndex);
			final SimpsonRealIntegrator integrtor = new SimpsonRealIntegrator(t1, t2, numberOfEvaluationPoints, true);
			final DoubleUnaryOperator integrand = t -> Math.pow((a + b * (maturity-t)) * Math.exp(-c * (maturity-t)) + d,2);
			final double variance = integrtor.integrate(integrand);
			final double volatilityNumerical = Math.sqrt(variance/(t2-t1));

			/*
			 * Measure a relative error if v1 > 1, otherwise an absolute error.
			 */
			error = Math.max(error, Math.abs(volatilityAnalytical-volatilityNumerical)/Math.max(volatilityAnalytical, 1));

			//			System.out.println(error + "\t" + timeIndex + "\t" + liborIndex + "\t" + t1 + "\t" + t2 + "\t" + maturity + "\t" + volatilityAnalytical + "\t" + volatilityNumerical +"\t" + a + "\t" + b + "\t" + c + "\t" + d);

			/*
			 * Mostly the approximation accuracy is around 1E-11, sometimes 1E-10
			 */
			Assert.assertEquals(0.0, error, 2E-10);
		}

	}

}
