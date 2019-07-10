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
		Random random = new Random(3141);

		double aMin = 0.0;
		double aMax = 1.0;
		double bMin = 0.0;
		double bMax = 2.0;
		double cMin = -0.1;
		double cMax = 1.0;
		double dMin = 0.0;
		double dMax = 0.2;

		/*
		 * Difficult values are small values for c, e.g.
		 * c =  1.6442404690564238E-6;
		 * c =  7.1699989962897840E-6;
		 * c = -8.5350795667182840E-8;
		 */
		double error = 0.0;
		for(int i=0; i<10000; i++) {
			double a = aMin + random.nextDouble() * (aMax-aMin);
			double b = bMin + random.nextDouble() * (bMax-bMin);
			double c = cMin + random.nextDouble() * (cMax-cMin);
			double d = dMin + random.nextDouble() * (dMax-dMin);

			int numberOfTimePoints = 100;
			TimeDiscretization td = new TimeDiscretizationFromArray(0.0, numberOfTimePoints, 50.0/numberOfTimePoints);
			LIBORVolatilityModel volatilityModel = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(td, td, a, b, c, d, false);

			int timeIndex = random.nextInt(numberOfTimePoints-1);
			int liborIndex = random.nextInt(numberOfTimePoints-timeIndex-1)+1;

			double volatilityAnalytical = volatilityModel.getVolatility(timeIndex, timeIndex+liborIndex).getAverage();

			/*
			 * Numerical integration
			 */
			int numberOfEvaluationPoints = 10000;
			double t1 = td.getTime(timeIndex);
			double t2 = td.getTime(timeIndex+1);
			double maturity = td.getTime(timeIndex+liborIndex);
			SimpsonRealIntegrator integrtor = new SimpsonRealIntegrator(t1, t2, numberOfEvaluationPoints, true);
			DoubleUnaryOperator integrand = t -> Math.pow((a + b * (maturity-t)) * Math.exp(-c * (maturity-t)) + d,2);
			double variance = integrtor.integrate(integrand);
			double volatilityNumerical = Math.sqrt(variance/(t2-t1));

			/*
			 * Measure a relative error if v1 > 1, otherwise an absolute error.
			 */
			error = Math.max(error, Math.abs(volatilityAnalytical-volatilityNumerical)/Math.max(volatilityAnalytical, 1));

//			System.out.println(error + "\t" + timeIndex + "\t" + liborIndex + "\t" + t1 + "\t" + t2 + "\t" + maturity + "\t" + volatilityAnalytical + "\t" + volatilityNumerical +"\t" + a + "\t" + b + "\t" + c + "\t" + d);

			/*
			 * Mostly the approximation accuracy is around 1E-11, sometimes 1E-8
			 */
			Assert.assertEquals(0.0, error, 1E-5);
		}

	}

}
