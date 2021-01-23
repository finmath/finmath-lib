package net.finmath.equities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.Test;

import net.finmath.equities.marketdata.AffineDividend;
import net.finmath.equities.marketdata.AffineDividendStream;
import net.finmath.equities.marketdata.FlatYieldCurve;
import net.finmath.equities.models.BuehlerDividendForwardStructure;
import net.finmath.equities.models.FlatVolatilitySurface;
import net.finmath.equities.models.SviVolatilitySmile;
import net.finmath.equities.models.SviVolatilitySurface;
import net.finmath.equities.pricer.AnalyticOptionValuation;
import net.finmath.equities.pricer.PdeOptionValuation;
import net.finmath.equities.products.AmericanOption;
import net.finmath.equities.products.EuropeanOption;
import net.finmath.equities.products.Option;
import net.finmath.exception.CalculationException;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConventionFactory;

/**
 * Tests for the PDE option pricer under a lognormal model with Buehler dividends.
 *
 * @author Andreas Grotz
 */

public class PdeOptionPricerTest {
	/*
	 */
	static final DecimalFormat decform = new DecimalFormat("#0.00");
	private final DayCountConvention dcc = DayCountConventionFactory.getDayCountConvention("act/365") ;

	@Test
	public void Test_pricer_european() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test European option price");
		System.out.println("===========================================");

		final var anaPricer = new AnalyticOptionValuation(dcc);
		final var pdePricer = new PdeOptionValuation(0.1, 5.0, 50, 30, dcc, false, false);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 100.0;
		final var volatility = 0.25;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.05;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 12.0, 0.03),});

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var expiryDate = LocalDate.parse("2020-12-24");
		final var strike = 90.0;

		final boolean[] callput = {true, false};
		for (final var isCall : callput)
		{
			final var option = new EuropeanOption(expiryDate, strike, isCall);

			final var anaPrice = anaPricer.getPrice(option, fwdStructure, curve, flatVol);
			final var pdePrice = pdePricer.getPrice(option, fwdStructure, curve, flatVol);

			System.out.println("Ana " + (isCall ? "Call" : "Put") + " : " + anaPrice);
			System.out.println("Pde " + (isCall ? "Call" : "Put") + " : " + pdePrice);
			System.out.println();

			assertEquals("Pde price deviates to much from analytic price for "  + (isCall ? "Call" : "Put"),
					0.0, pdePrice/anaPrice - 1.0, 0.005);
		}
	}

	@Test
	public void Test_pricer_american() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test American option price");
		System.out.println("===========================================");

		final var anaPricer = new AnalyticOptionValuation(dcc);
		final var pdePricer = new PdeOptionValuation(0.1, 3.0, 50, 30, dcc, false, true);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 100.0;
		final var volatility = 0.25;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.05;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 5.0, 0.00),
						new AffineDividend(LocalDate.parse("2020-09-17"), 3.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 0.0, 0.05),});

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var expiryDate = LocalDate.parse("2021-10-15");
		final var strike = 100.0;

		final boolean[] callput = {true, false};
		for (final var isCall : callput)
		{
			final var americanOption = new AmericanOption(expiryDate, strike, isCall);
			final var europeanOption = new EuropeanOption(expiryDate, strike, isCall);

			final var anaPrice = anaPricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
			final var pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
			final var pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

			System.out.println("Ana Eu " + (isCall ? "Call" : "Put") + " : " + anaPrice);
			System.out.println("Pde Eu " + (isCall ? "Call" : "Put") + " : " + pdePriceEu);
			System.out.println("Pde Am " + (isCall ? "Call" : "Put") + " : " + pdePriceAm);
			System.out.println();

			assertTrue("American "  + (isCall ? "Call" : "Put") + " must have higher price than European",
					pdePriceAm >= pdePriceEu);
		}
	}

	@Test
	public void Test_pricer_americanCall() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test American call early exercise");
		System.out.println("==================================================");

		final var pdePricer = new PdeOptionValuation(0.1, 3.0, 50, 30, dcc, false, false);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 100.0;
		final var volatility = 0.0000001;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.0;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-06-15"), 10.0, 0.00),});

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var expiryDate = LocalDate.parse("2021-06-15");
		final var strike = 90.0;

		final var americanOption = new AmericanOption(expiryDate, strike, true);
		final var europeanOption = new EuropeanOption(expiryDate, strike, true);
		final var pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
		final var pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

		System.out.println("Pde Eu Call: " + pdePriceEu);
		System.out.println("Pde Am Call: " + pdePriceAm);
		System.out.println();

		assertEquals("American call should be equal to intrinsic value",
				10.0, pdePriceAm, 1e-5);
		assertEquals("European call should be worthless",
				0.0, pdePriceEu, 1e-5);
	}

	@Test
	public void Test_pricer_americanPut() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test American put early exercise");
		System.out.println("=================================================");

		final var pdePricer = new PdeOptionValuation(0.1, 3.0, 50, 30, dcc, false, false);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 50.0;
		final var volatility = 0.5;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.25;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[0]);

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var expiryDate = LocalDate.parse("2021-06-15");
		final var strike = 100.0;

		final var americanOption = new AmericanOption(expiryDate, strike, false);
		final var europeanOption = new EuropeanOption(expiryDate, strike, false);
		final var pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
		final var pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

		System.out.println("Pde Eu Put: " + pdePriceEu);
		System.out.println("Pde Am Put: " + pdePriceAm);
		System.out.println();

		assertEquals("American put should be equal to intrinsic value",
				50.0, pdePriceAm, 1e-2);
		assertEquals("European put should be equal to option value",
				21.11, pdePriceEu, 1e-2);
	}

	@Test
	public void Test_pricer_lv() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test local volatility price");
		System.out.println("============================================");

		final var pdePricer = new PdeOptionValuation(0.1, 3.0, 75, 50, dcc, false, false);
		final var pdeLvPricer = new PdeOptionValuation(0.1, 3.0, 75, 50, dcc, true, false);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 100.0;
		final var rate = 0.05;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.03),});

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var smile = new SviVolatilitySmile(LocalDate.parse("2021-06-15"), 0.0078, 0.052, -0.449, 0.356, 0.348);
		final var smiles = new SviVolatilitySmile[] {smile, };
		final var surface = new SviVolatilitySurface(valDate, dcc, fwdStructure, smiles, false);

		final var expiryDate = LocalDate.parse("2020-12-24");
		final var strike = 90.0;

		final var volatility = surface.getVolatility(strike, expiryDate, fwdStructure);

		final boolean[] callOrPut = {true, false};
		final boolean[] americanOrEuropean = {true, false};
		for (final var isCall : callOrPut)
		{
			for (final var isAmerican : americanOrEuropean)
			{
				Option option;
				if (isAmerican) {
					option = new AmericanOption(expiryDate, strike, isCall);
				} else {
					option = new EuropeanOption(expiryDate, strike, isCall);
				}

				final var pdePrice = pdePricer.getPrice(option, fwdStructure, curve, surface);
				final var pdeLvPrice = pdeLvPricer.getPrice(option, fwdStructure, curve, surface);

				System.out.println("Vol: " + volatility);
				System.out.println("Pde " + (isAmerican ? "American " : "European ") + (isCall ? "Call" : "Put") + " : " + pdePrice);
				System.out.println("Pde LV " + (isAmerican ? "American " : "European ") + (isCall ? "Call" : "Put") + " : " + pdeLvPrice);
				System.out.println();

				assertEquals("Pde price deviates to much from analytic price for "
						+ (isAmerican ? "American " : "European ")
						+ (isCall ? "Call" : "Put"),
						0.0, pdeLvPrice/pdePrice - 1.0, isAmerican ? 0.02 : 0.005);
			}
		}
	}

	@Test
	public void Test_noArbitrage() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test American option arbitrage");
		System.out.println("===============================================");

		final var pricer = new PdeOptionValuation(0.1, 3.0, 50, 40, dcc, false, false);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 100.0;
		final var volatility = 0.25;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.05;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
						new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0),});

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var expiryDateBefore = LocalDate.parse("2020-09-16");
		final var strike = 100.0;
		final var expiryDateAfter = LocalDate.parse("2020-09-17");

		final boolean isCall = true;
		final var optionBefore = new AmericanOption(expiryDateBefore, strike, isCall);
		final var optionAfter = new AmericanOption(expiryDateAfter, strike, isCall);

		final var priceBefore = pricer.getPrice(optionBefore, fwdStructure, curve, flatVol);
		final var priceAfter = pricer.getPrice(optionAfter, fwdStructure, curve, flatVol);

		System.out.println("Price " + (isCall ? "Call" : "Put") + " before: " + priceBefore);
		System.out.println("Price " + (isCall ? "Call" : "Put") + " after: " + priceAfter);
		System.out.println();

		assertEquals("Price before and after dividend should be almost equal",
				0.0, priceAfter/priceBefore - 1.0, 0.015);
	}

	@Test
	public void Test_europeanSensis() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test European Greeks");
		System.out.println("==================================================");

		final var anaPricer = new AnalyticOptionValuation(dcc);
		final var pdePricer = new PdeOptionValuation(0.1, 5.0, 50, 30, dcc, false, false);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 100.0;
		final var volatility = 0.35;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.15;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 12.0, 0.02),
						new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.04),});

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var expiryDate = LocalDate.parse("2020-06-15");
		final var strike = 100.0;

		final boolean[] callput = {true, false};
		for (final var isCall : callput)
		{
			final var option = new EuropeanOption(expiryDate, strike, isCall);

			final var pdeSensis = pdePricer.getPdeSensis(option, fwdStructure, curve, flatVol);
			final var pdeVega = pdePricer.getVega(option, fwdStructure, curve, flatVol, pdeSensis[0], 1e-3);
			final var anaDelta = anaPricer.getDelta(option, fwdStructure, curve, flatVol);
			final var anaGamma = anaPricer.getGamma(option, fwdStructure, curve, flatVol);
			final var anaTheta = anaPricer.getTheta(option, fwdStructure, curve, flatVol);
			final var anaVega = anaPricer.getVega(option, fwdStructure, curve, flatVol);

			System.out.println("Ana Delta " + (isCall ? "Call" : "Put") + " : " + anaDelta);
			System.out.println("Pricer Delta " + (isCall ? "Call" : "Put") + " : " + pdeSensis[1]);
			System.out.println("Ana Gamma " + (isCall ? "Call" : "Put") + " : " + anaGamma);
			System.out.println("Pricer Gamma " + (isCall ? "Call" : "Put") + " : " + pdeSensis[2]);
			System.out.println("Ana Vega " + (isCall ? "Call" : "Put") + " : " + anaVega);
			System.out.println("Pricer Vega " + (isCall ? "Call" : "Put") + " : " + pdeVega);
			System.out.println("Ana Theta " + (isCall ? "Call" : "Put") + " : " + anaTheta);
			System.out.println("Pricer Theta " + (isCall ? "Call" : "Put") + " : " + pdeSensis[3]);
			System.out.println();

			assertEquals("Pde Delta deviates to much from analytic value for "  + (isCall ? "Call" : "Put"),
					0.0, pdeSensis[1]/anaDelta - 1.0, 0.01);
			assertEquals("Pde Gamma deviates to much from analytic value for "  + (isCall ? "Call" : "Put"),
					0.0, pdeSensis[2]/anaGamma - 1.0, 0.01);
			assertEquals("Pde Vega deviates to much from analytic value for "  + (isCall ? "Call" : "Put"),
					0.0, pdeVega/anaVega - 1.0, 0.02);
			assertEquals("Pde Theta deviates to much from analytic value for "  + (isCall ? "Call" : "Put"),
					0.0, pdeSensis[3]/anaTheta - 1.0, 0.02);
		}
	}

	@Test
	public void Test_impliedVol() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test implied volatility");
		System.out.println("========================================");

		final var pdePricer = new PdeOptionValuation(0.1, 5.0, 50, 30, dcc, false, false);
		final var valDate = LocalDate.parse("2019-06-15");
		final var spot = 100.0;
		final var volatility = 0.35;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.05;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 12.0, 0.02),
						new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.04),});

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final var expiryDate = LocalDate.parse("2021-06-15");
		final var strike =100.0;

		final boolean[] callput = {true, false};
		for (final var isCall : callput)
		{
			final var option = new AmericanOption(expiryDate, strike, isCall);
			final var price = pdePricer.getPrice(option, fwdStructure, curve, flatVol);
			final var impVol = pdePricer.getImpliedVolatility(option, fwdStructure, curve, price);
			final var priceFromImpVol = pdePricer.getPrice(option, fwdStructure, curve, new FlatVolatilitySurface(impVol));

			System.out.println("Price " + (isCall ? "Call" : "Put") + " : " + price);
			System.out.println("Price from impl vol " + (isCall ? "Call" : "Put") + " : " + priceFromImpVol);
			System.out.println("Input vol " + (isCall ? "Call" : "Put") + " : " + volatility);
			System.out.println("Implied vol " + (isCall ? "Call" : "Put") + " : " + impVol);
			System.out.println();

			assertEquals("Price from implied vol deviates to much from input price for "  + (isCall ? "Call" : "Put"),
					0.0, priceFromImpVol/price - 1.0, 0.01);
		}
	}

	//@Test // Uncomment this line to run and print sensitivities for a stability analysis.
	public void Test_sensiStability() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Provide sensitivities for stability analysis");
		System.out.println("=============================================================");

		final var pdePricer = new PdeOptionValuation(0.1, 3.0, 70, 0, dcc, false, true);
		final var valDate = LocalDate.parse("2019-06-15");
		final var volatility = 0.35;
		//var volatility = 0.2;
		final var flatVol = new FlatVolatilitySurface(volatility);
		final var rate = 0.05;
		//var rate = 0.1;
		final var curve = new FlatYieldCurve(valDate, rate, dcc);

		final var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 5.0, 0.00),
						new AffineDividend(LocalDate.parse("2020-09-17"), 3.0, 0.02),
						new AffineDividend(LocalDate.parse("2021-09-17"), 0.0, 0.045),});
		//var dividends = new AffineDividendStream(new AffineDividend[0]);

		final var fwdStructure = new BuehlerDividendForwardStructure(valDate, 100.0, curve, dividends, dcc);

		final var expiryDate = LocalDate.parse("2020-12-15");
		//var expiryDate = LocalDate.parse("2019-09-15");
		final var strike = 100.0;

		final boolean[] callput = {true, false};
		final boolean[] american = {true, false};
		final var spots = new ArrayList<Double>();
		for (int i = 50; i <= 150; i++) {
			spots.add(1.0 * i);
		}
		//spots = new ArrayList<Double>() {{add(80.0);}};
		System.out.println("Exercise,Type,Spot,Price,Delta,Gamma,Vega,Theta");
		for (final var isAmerican : american)
		{
			for (final var isCall : callput)
			{
				for (final var spot : spots)
				{
					final var thisStructure = fwdStructure.cloneWithNewSpot(spot);
					Option option;
					if(isAmerican) {
						option = new AmericanOption(expiryDate, strike, isCall);
					} else {
						option = new EuropeanOption(expiryDate, strike, isCall);
					}

					final var pdeSensis = pdePricer.getPdeSensis(option, thisStructure, curve, flatVol);
					final var pdeVega = pdePricer.getVega(option, thisStructure, curve, flatVol, pdeSensis[0], 1e-6);
					//var pdeTheta = pdePricer.getTheta(option, thisStructure, curve, flatVol, pdeSensis[0]);

					System.out.println((isAmerican ? "American" : "European") + "," + (isCall ? "Call" : "Put") + "," + spot +"," + pdeSensis[0] + "," + pdeSensis[1] + "," + pdeSensis[2] + "," + pdeVega + "," + pdeSensis[3]);
				}
			}
		}
		System.out.println();
	}
}
