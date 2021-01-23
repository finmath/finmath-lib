package net.finmath.equities;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import net.finmath.equities.models.Black76Model;
import net.finmath.exception.CalculationException;

/**
 * Tests for the analytic formulas of the Black76 model.
 *
 * @author Andreas Grotz
 */
public class Black76ModelTest {
	/*
	 */

	@Test
	public void Test_black76_formula() throws CalculationException
	{
		final var forward = 0.05;
		final var forward2 = 0.1;
		final var strike = 0.01;
		final var strike2 = 0.3;
		final var maturity = 10.0;
		final var volatility = 0.25;
		final var discountFactor = 1.0;

		// regular case
		assertEquals(0.040128147990592215,
				Black76Model.optionPrice(forward, strike, maturity, volatility, true, discountFactor),
				1E-16);

		// put-call parity
		assertEquals(
				Black76Model.optionPrice(forward, forward, maturity, volatility, true, discountFactor),
				Black76Model.optionPrice(forward, forward, maturity, volatility, false, discountFactor),
				1E-16);

		// deterministic cases
		assertEquals(forward - strike, Black76Model.optionPrice(forward, strike, maturity, 0.0, true, discountFactor), 1E-16);
		assertEquals(strike2 - forward2, Black76Model.optionPrice(forward2, strike2, 0.0, volatility, false, discountFactor), 1E-16);
	}

	// Check implied vol results for the four plots in Figure 3 of the Jaeckel paper
	@Test
	public void Test_black76_impliedVolatility()
	{
		final var relTolVol = 1e-14;
		final var relTolPv = 1e-13;

		final var fwd = 1.0;
		final var ttm = 1.0;
		final var discountFactor = 1.0;

		final var lmList = new double[] {1.0 / 16.0, 1.0, 8.0, 64.0, };
		final var vol1List = new double[] {0.05, 0.5, 2.5, 9.5, };
		final var vol2List = new double[] {0.1, 1.0, 3.5, 10.5, };
		final var vol3List = new double[] {0.5, 2.5, 4.5, 12.0, };
		final var vol4List = new double[] {3.0, 4.0, 7.5, 14.0, };

		for (int i=0; i<=3; i++)
		{
			final var logMoneyness = lmList[i];
			final var vol1 = vol1List[i];
			final var vol2 = vol2List[i];
			final var vol3 = vol3List[i];
			final var vol4 = vol4List[i];
			final String plotNb = Integer.toString(i);
			final double stk = fwd * Math.exp(logMoneyness);

			// Branch 1
			final var pv1 = Black76Model.optionPrice(fwd, stk, ttm, vol1, true, discountFactor);
			final var impVol1 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv1, true);
			assertEquals("Vol not good for branch 1 in plot " + plotNb,
					impVol1 / vol1,
					1.0,
					relTolVol);
			final var pvVol1 = Black76Model.optionPrice(fwd, stk, ttm, impVol1, true, discountFactor);
			assertEquals("Pv not good for branch 1 in plot " + plotNb,
					pvVol1 / pv1,
					1.0,
					relTolPv);
			// Branch 2
			final var pv2 = Black76Model.optionPrice(fwd, stk, ttm, vol2, true, discountFactor);
			final var impVol2 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv2, true);
			assertEquals("Vol not good for branch 2 in plot " + plotNb,
					impVol2 / vol2,
					1.0,
					relTolVol);
			final var pvVol2 = Black76Model.optionPrice(fwd, stk, ttm, impVol2, true, discountFactor);
			assertEquals("Pv not good for branch 2 in plot " + plotNb,
					pvVol2 / pv2,
					1.0,
					relTolPv);
			// Branch 3
			final var pv3 = Black76Model.optionPrice(fwd, stk, ttm, vol3, true, discountFactor);
			final var impVol3 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv3, true);
			assertEquals("Vol not good for branch 3 in plot " + plotNb,
					impVol3 / vol3,
					1.0,
					relTolVol);
			final var pvVol3 = Black76Model.optionPrice(fwd, stk, ttm, impVol3, true, discountFactor);
			assertEquals("Pv not good for branch 3 in plot " + plotNb,
					pvVol3 / pv3,
					1.0,
					relTolPv);
			// Branch 4
			final var pv4 = Black76Model.optionPrice(fwd, stk, ttm, vol4, true, discountFactor);
			final var impVol4 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv4, true);
			assertEquals("Vol not good for branch 4 in plot " + plotNb,
					impVol4 / vol4,
					1.0,
					relTolVol);
			final var pvVol4 = Black76Model.optionPrice(fwd, stk, ttm, impVol4, true, discountFactor);
			assertEquals("Pv not good for branch 4 in plot " + plotNb,
					pvVol4 / pv4,
					1.0,
					relTolPv);
		}
	}
}
