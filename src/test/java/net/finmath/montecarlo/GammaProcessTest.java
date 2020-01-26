/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 05.07.2014
 */
package net.finmath.montecarlo;

import java.text.DecimalFormat;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * @author Christian Fries
 *
 */
public class GammaProcessTest {

	static final DecimalFormat formatterReal2	= new DecimalFormat(" 0.00");
	static final DecimalFormat formatterSci4	= new DecimalFormat(" 0.0000E00;-0.0000E00");
	static final DecimalFormat formatterSci1	= new DecimalFormat(" 0E00;-0.E00");

	@Test
	public void testScaling() {
		// The parameters
		final int seed			= 53252;
		final int numberOfPaths	= 10000;
		final double lastTime		= 10;
		final double dt			= 0.1;

		System.out.println("Test of scaling of Gamma increments.");

		// Create the time discretization
		final TimeDiscretization timeDiscretization = new TimeDiscretizationFromArray(0.0, (int)(lastTime/dt), dt);

		final double shape = 3.0;
		final double scale1 = 1.0;
		final double scale2 = 2.0;

		final IndependentIncrements gamma1 = new GammaProcess(
				timeDiscretization,
				1,
				numberOfPaths,
				seed,
				shape,
				scale1	// Scale
				);

		final IndependentIncrements gamma2 = new GammaProcess(
				timeDiscretization,
				1,
				numberOfPaths,
				seed,
				shape,
				scale2	// Scale
				);

		final RandomVariable gammaIncement1 = gamma1.getIncrement(3, 0);
		final RandomVariable gammaIncement2 = gamma2.getIncrement(3, 0);

		for(int i=0; i<gammaIncement1.size(); i++) {
			final double diff = gammaIncement2.get(i) - scale2/scale1 * gammaIncement1.get(i);
			System.out.println(formatterReal2.format(gammaIncement1.get(i)) + "\t" + formatterReal2.format(gammaIncement2.get(i)) + "\t" + formatterSci4.format(diff));
			Assert.assertTrue(Math.abs(diff) < 1E-8);
		}
	}
}
