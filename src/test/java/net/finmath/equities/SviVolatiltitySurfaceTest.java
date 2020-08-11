package net.finmath.equities;

import static org.junit.Assert.assertEquals;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.Test;

import net.finmath.equities.marketdata.AffineDividend;
import net.finmath.equities.marketdata.AffineDividendStream;
import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.equities.marketdata.VolatilityPoint;
import net.finmath.equities.models.BuehlerDividendForwardStructure;
import net.finmath.equities.models.SviVolatilitySmile;
import net.finmath.equities.models.SviVolatilitySurface;
import net.finmath.exception.CalculationException;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConventionFactory;

/**
 * Tests for the SVI volatility surface implementation.
 *
 * @author Andreas Grotz
 */

public class SviVolatiltitySurfaceTest {
	/*
	 */
	private static final DecimalFormat decform = new DecimalFormat("#0.00");
	private static final DayCountConvention dcc = DayCountConventionFactory.getDayCountConvention("act/365") ;

	@Test
	public void Test_noArbitrage() throws CalculationException
	{
		System.out.println("SviVolatiltitySurface: Test for arbitrage");
		System.out.println("========================================");

		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var rate = 0.01;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 20.0, 0.0),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var smileBefore = new SviVolatilitySmile(LocalDate.parse("2020-09-01"), 0.02, 0.01, 0.0, 0.0, 0.001);
		var smileAfter = new SviVolatilitySmile(LocalDate.parse("2020-10-01"), 0.04, 0.01, 0.0, 0.0, 0.001);
		var smiles = new SviVolatilitySmile[] {smileBefore, smileAfter};
		var surface = new SviVolatilitySurface(valDate, dcc, fwdStructure, smiles, false);

		var expiryDateBefore = LocalDate.parse("2020-09-16");
		var strikeBefore = 100.0;
		var expiryDateAfter = LocalDate.parse("2020-09-17");
		var strikeAfter = 80.0;

		var volBefore = surface.getVolatility(strikeBefore, expiryDateBefore, fwdStructure);
		var volAfter = surface.getVolatility(strikeAfter, expiryDateAfter, fwdStructure);

		System.out.println("Vol before: " + volBefore);
		System.out.println("Vol after: " + volAfter);
		System.out.println();

		assertEquals(0.0, volAfter / volBefore - 1.0, 0.01);
	}

	@Test
	public void Test_stickyness() throws CalculationException
	{
		System.out.println("SviVolatiltitySurface: Test stickyness");
		System.out.println("======================================");

		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var rate = 0.0;
		var curve = new FlatYieldCurve(valDate, rate, dcc);
		var dividends = new AffineDividendStream(new AffineDividend[0]);
		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var smileBefore = new SviVolatilitySmile(LocalDate.parse("2020-09-01"), 0.02, 0.01, 0.0, 0.0, 0.001);
		var smileAfter = new SviVolatilitySmile(LocalDate.parse("2020-10-01"), 0.04, 0.01, 0.0, 0.0, 0.001);
		var smiles = new SviVolatilitySmile[] {smileBefore, smileAfter};
		var surfaceStickyMoneyness = new SviVolatilitySurface(valDate, dcc, fwdStructure, smiles, false);
		var surfaceStickyStrike = new SviVolatilitySurface(valDate, dcc, fwdStructure, smiles, true);

		var strike = 80.0;

		var newSpot = 1.1 * spot;
		var newFwdStructure = new BuehlerDividendForwardStructure(valDate, newSpot, curve, dividends, dcc);
		var newStrikeForStickyMoneyness = 1.1 * strike;
		var testDate = LocalDate.parse("2020-09-20");

		var volStickyMoneyness = surfaceStickyMoneyness.getVolatility(strike, testDate, fwdStructure);
		var newVolStickyMoneyness = surfaceStickyMoneyness.getVolatility(newStrikeForStickyMoneyness, testDate, newFwdStructure);
		var volStickyStrike = surfaceStickyStrike.getVolatility(strike, testDate, fwdStructure);
		var newVolStickyStrike = surfaceStickyStrike.getVolatility(strike, testDate, newFwdStructure);

		System.out.println("Vol sticky money: " + volStickyMoneyness);
		System.out.println("New vol sticky money: " + newVolStickyMoneyness);
		System.out.println("Vol sticky strike: " + volStickyStrike);
		System.out.println("New vol sticky strike: " + newVolStickyStrike);

		var localVolStickyMoneyness = surfaceStickyMoneyness.getLocalVolatility(strike, testDate, fwdStructure, 1e-4, 1e-4);
		var newLocalVolStickyMoneyness = surfaceStickyMoneyness.getLocalVolatility(newStrikeForStickyMoneyness, testDate, newFwdStructure, 1e-4, 1e-4);
		var localVolStickyStrike = surfaceStickyStrike.getLocalVolatility(strike, testDate, fwdStructure, 1e-4, 1e-4);
		var newLocalVolStickyStrike = surfaceStickyStrike.getLocalVolatility(strike, testDate, newFwdStructure, 1e-4, 1e-4);

		System.out.println("LV sticky money: " + localVolStickyMoneyness);
		System.out.println("New LV sticky money: " + newLocalVolStickyMoneyness);
		System.out.println("LV sticky strike: " + localVolStickyStrike);
		System.out.println("New LV sticky strike: " + newLocalVolStickyStrike);
		System.out.println();

		assertEquals(0.0, newVolStickyMoneyness / volStickyMoneyness - 1.0, 1e-16);
		assertEquals(0.0, newVolStickyStrike / volStickyStrike - 1.0, 1e-16);
		assertEquals(0.0, newLocalVolStickyMoneyness / localVolStickyMoneyness - 1.0, 1e-16);
		assertEquals(0.0, newLocalVolStickyStrike / localVolStickyStrike - 1.0, 1e-16);
	}

	@Test
	public void Test_calibration() throws CalculationException
	{
		System.out.println("SviVolatiltitySurface: Test calibration");
		System.out.println("=======================================");

		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var rate = 0.01;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var a = 0.0078;
		var b = 0.052;
		var rho = -0.449;
		var m = 0.0356;
		var sigma = 0.348;

		var logStrikes = new ArrayList<Double>();
		var totalVariances = new ArrayList<Double>();
		for (int i = -20; i <= 20; i++)
		{
			var strike = 0.1 * i;
			logStrikes.add(strike);
			totalVariances.add(SviVolatilitySmile.sviTotalVariance(strike, a, b, rho, m, sigma));
		}
		var initialGuess = SviVolatilitySmile.sviInitialGuess(logStrikes, totalVariances);

		var smileDate = LocalDate.parse("2021-06-15");
		var smile = new SviVolatilitySmile(smileDate, a, b, rho, m, sigma);
		var smiles = new SviVolatilitySmile[] {smile, };
		var surface = new SviVolatilitySurface(valDate, dcc, fwdStructure, smiles, false);
		var forward = fwdStructure.getForward(smileDate);

		var strikes = new ArrayList<Double>();
		var volaPoints = new ArrayList<VolatilityPoint>();
		for (int i = -5; i <= 5; i++)
		{
			strikes.add(forward + 5 * i);
			volaPoints.add(new VolatilityPoint(smileDate, forward + 5 * i,
					surface.getVolatility(forward + 5 * i, smileDate, fwdStructure)));
		}
		var newSurface = new SviVolatilitySurface(dcc, false);
		newSurface.calibrate(fwdStructure, volaPoints);

		var newSmile = newSurface.getSmiles()[0];

		System.out.println("a in: " + a);
		System.out.println("a guess: " + initialGuess[0]);
		System.out.println("a calib: " + newSmile.getA());
		System.out.println("b in: " + b);
		System.out.println("b guess: " + initialGuess[1]);
		System.out.println("b calib: " + newSmile.getB());
		System.out.println("rho in: " + rho);
		System.out.println("rho guess: " + initialGuess[2]);
		System.out.println("rho calib: " + newSmile.getRho());
		System.out.println("m in: " + m);
		System.out.println("m guess: " + initialGuess[3]);
		System.out.println("m calib: " + newSmile.getM());
		System.out.println("sigma in: " + sigma);
		System.out.println("sigma guess: " + initialGuess[4]);
		System.out.println("sigma calib: " + newSmile.getSigma());
		System.out.println();

		assertEquals(0.0, initialGuess[0] / a - 1.0, 1e-1);
		assertEquals(0.0, newSmile.getA() / a - 1.0, 1e-6);
		assertEquals(0.0, initialGuess[1] / b - 1.0, 1e-1);
		assertEquals(0.0, newSmile.getB() / b - 1.0, 1e-6);
		assertEquals(0.0, initialGuess[2] / rho - 1.0, 1e-1);
		assertEquals(0.0, newSmile.getRho() / rho - 1.0, 1e-6);
		assertEquals(0.0, initialGuess[3] / m - 1.0, 1e-1);
		assertEquals(0.0, newSmile.getM() / m - 1.0, 1e-6);
		assertEquals(0.0, initialGuess[4] / sigma - 1.0, 1e-1);
		assertEquals(0.0, newSmile.getSigma() / sigma - 1.0, 1e-6);
	}
}
