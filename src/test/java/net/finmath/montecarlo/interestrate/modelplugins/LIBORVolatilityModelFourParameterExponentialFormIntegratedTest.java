/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.06.2016
 */

package net.finmath.montecarlo.interestrate.modelplugins;

import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Unit test for <code>LIBORVolatilityModelFourParameterExponentialFormIntegrated</code>.
 *
 * This test shows that the analytic formula used in <code>LIBORVolatilityModelFourParameterExponentialFormIntegrated</code>
 * - in the limit - agrees with the result obtained from <code>LIBORVolatilityModelFourParameterExponentialForm</code>
 * given that its time discretization step size is going to zero.
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
		double cMin = 0.1;
		double cMax = 1.0;
		double dMin = 0.0;
		double dMax = 0.2;

		double error = 0.0;
		for(int i=0; i<10000; i++) {
			double a = aMin + random.nextDouble() * (aMax-aMin);
			double b = bMin + random.nextDouble() * (bMax-bMin);
			double c = cMin + random.nextDouble() * (cMax-cMin);
			double d = dMin + random.nextDouble() * (dMax-dMin);

			int numberOfTimePoints = 20000;
			TimeDiscretizationInterface td = new TimeDiscretization(0.0, numberOfTimePoints, 10.0/numberOfTimePoints);
			LIBORVolatilityModelFourParameterExponentialFormIntegrated vol1 = new LIBORVolatilityModelFourParameterExponentialFormIntegrated(td, td, a, b, c, d, false);
			LIBORVolatilityModelFourParameterExponentialForm vol2 = new LIBORVolatilityModelFourParameterExponentialForm(td, td, a, b, c, d, false);

			int timeIndex = random.nextInt(numberOfTimePoints-1);
			int liborIndex = random.nextInt(numberOfTimePoints-timeIndex-1)+1;

			double v1 = vol1.getVolatility(timeIndex, timeIndex+liborIndex).getAverage();
			double v2 = vol2.getVolatility(timeIndex, timeIndex+liborIndex).getAverage();

			error = Math.max(error, Math.abs(v1-v2));
		}

		System.out.println(error);
		Assert.assertEquals(0.0, error, 1E-3);
	}

}

