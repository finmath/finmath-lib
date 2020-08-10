package net.finmath.equities;

import static org.junit.Assert.*;
import org.junit.Test;
import net.finmath.exception.CalculationException;
import net.finmath.equities.models.*;

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
		var forward = 0.05;
		var forward2 = 0.1;
		var strike = 0.01;
		var strike2 = 0.3;
		var maturity = 10.0;
		var volatility = 0.25;
		var discountFactor = 1.0;

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
		var relTolVol = 1e-14;
		var relTolPv = 1e-13;

		var fwd = 1.0;
		var ttm = 1.0;
		var discountFactor = 1.0;

		var lmList = new double[] {1.0 / 16.0, 1.0, 8.0, 64.0, };
		var vol1List = new double[] {0.05, 0.5, 2.5, 9.5, };
		var vol2List = new double[] {0.1, 1.0, 3.5, 10.5, };
		var vol3List = new double[] {0.5, 2.5, 4.5, 12.0, };
		var vol4List = new double[] {3.0, 4.0, 7.5, 14.0, };

		for (int i=0; i<=3; i++)
		{
			var logMoneyness = lmList[i];
			var vol1 = vol1List[i];
			var vol2 = vol2List[i];
			var vol3 = vol3List[i];
			var vol4 = vol4List[i];
			final String plotNb = Integer.toString(i);
			final double stk = fwd * Math.exp(logMoneyness);

			// Branch 1
			var pv1 = Black76Model.optionPrice(fwd, stk, ttm, vol1, true, discountFactor);
			var impVol1 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv1, true);
			assertEquals("Vol not good for branch 1 in plot " + plotNb,
					impVol1 / vol1,
					1.0,
					relTolVol);
			var pvVol1 = Black76Model.optionPrice(fwd, stk, ttm, impVol1, true, discountFactor);
			assertEquals("Pv not good for branch 1 in plot " + plotNb,
					pvVol1 / pv1,
					1.0,
					relTolPv);
			// Branch 2
			var pv2 = Black76Model.optionPrice(fwd, stk, ttm, vol2, true, discountFactor);
			var impVol2 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv2, true);
			assertEquals("Vol not good for branch 2 in plot " + plotNb,
					impVol2 / vol2,
					1.0,
					relTolVol);
			var pvVol2 = Black76Model.optionPrice(fwd, stk, ttm, impVol2, true, discountFactor);
			assertEquals("Pv not good for branch 2 in plot " + plotNb,
					pvVol2 / pv2,
					1.0,
					relTolPv);
			// Branch 3
			var pv3 = Black76Model.optionPrice(fwd, stk, ttm, vol3, true, discountFactor);
			var impVol3 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv3, true);
			assertEquals("Vol not good for branch 3 in plot " + plotNb,
					impVol3 / vol3,
					1.0,
					relTolVol);
			var pvVol3 = Black76Model.optionPrice(fwd, stk, ttm, impVol3, true, discountFactor);
			assertEquals("Pv not good for branch 3 in plot " + plotNb,
					pvVol3 / pv3,
					1.0,
					relTolPv);
			// Branch 4
			var pv4 = Black76Model.optionPrice(fwd, stk, ttm, vol4, true, discountFactor);
			var impVol4 = Black76Model.optionImpliedVolatility(fwd, stk, ttm, pv4, true);
			assertEquals("Vol not good for branch 4 in plot " + plotNb,
					impVol4 / vol4,
					1.0,
					relTolVol);
			var pvVol4 = Black76Model.optionPrice(fwd, stk, ttm, impVol4, true, discountFactor);
			assertEquals("Pv not good for branch 4 in plot " + plotNb,
					pvVol4 / pv4,
					1.0,
					relTolPv);
		}
	}
}
