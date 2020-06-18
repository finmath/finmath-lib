/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 14.02.2015
 */

package net.finmath.functions;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * Unit test, testing that the Brownian increment is normal distributed
 * using the Jarque-Bera test.
 *
 * @author Christian Fries
 */
public class JarqueBeraTestTest {

	@Test
	public void test() {
		final BrownianMotion bm = new BrownianMotionFromMersenneRandomNumbers(new TimeDiscretizationFromArray(0.0, 1.0, 2.0), 1 /* numberOfFactors */, 10000 /* numberOfPaths */, 2342 /* seed */);

		final double test = (new JarqueBeraTest()).test(bm.getBrownianIncrement(0 /* timeIndex */, 0 /* factor */));

		Assert.assertTrue("Normal distributed (level of significance 0.10)", test < 4.6);
	}

}
