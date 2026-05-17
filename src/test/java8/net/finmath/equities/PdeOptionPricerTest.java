package net.finmath.equities;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;

import org.junit.Test;

import net.finmath.equities.models.VolatilitySurface;
import net.finmath.equities.models.EquityForwardStructure;
import net.finmath.equities.marketdata.YieldCurve;
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

		final AnalyticOptionValuation anaPricer = new AnalyticOptionValuation(dcc);
		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 5.0, 50, 30, dcc, false, false);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double volatility = 0.25;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.05;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 12.0, 0.03),});

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final LocalDate expiryDate = LocalDate.parse("2020-12-24");
		final double strike = 90.0;

		final boolean[] callput = {true, false};
		for (final boolean isCall : callput)
		{
			final Option option = new EuropeanOption(expiryDate, strike, isCall);

			final double anaPrice = anaPricer.getPrice(option, fwdStructure, curve, flatVol);
			final double pdePrice = pdePricer.getPrice(option, fwdStructure, curve, flatVol);

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

		final AnalyticOptionValuation anaPricer = new AnalyticOptionValuation(dcc);
		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 3.0, 50, 30, dcc, false, true);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double volatility = 0.25;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.05;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 5.0, 0.00),
						new AffineDividend(LocalDate.parse("2020-09-17"), 3.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 0.0, 0.05),});

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final LocalDate expiryDate = LocalDate.parse("2021-10-15");
		final double strike = 100.0;

		final boolean[] callput = {true, false};
		for (final boolean isCall : callput)
		{
			final Option americanOption = new AmericanOption(expiryDate, strike, isCall);
			final Option europeanOption = new EuropeanOption(expiryDate, strike, isCall);

			final double anaPrice = anaPricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
			final double pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
			final double pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

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

		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 3.0, 50, 30, dcc, false, false);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double volatility = 0.0000001;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.0;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-06-15"), 10.0, 0.00),});

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final LocalDate expiryDate = LocalDate.parse("2021-06-15");
		final double strike = 90.0;

		final Option americanOption = new AmericanOption(expiryDate, strike, true);
		final Option europeanOption = new EuropeanOption(expiryDate, strike, true);
		final double pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
		final double pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

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

		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 3.0, 50, 30, dcc, false, false);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 50.0;
		final double volatility = 0.5;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.25;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[0]);

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final LocalDate expiryDate = LocalDate.parse("2021-06-15");
		final double strike = 100.0;

		final Option americanOption = new AmericanOption(expiryDate, strike, false);
		final Option europeanOption = new EuropeanOption(expiryDate, strike, false);
		final double pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
		final double pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

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

		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 3.0, 75, 50, dcc, false, false);
		final PdeOptionValuation pdeLvPricer = new PdeOptionValuation(0.1, 3.0, 75, 50, dcc, true, false);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double rate = 0.05;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.03),});

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final SviVolatilitySmile smile = new SviVolatilitySmile(LocalDate.parse("2021-06-15"), 0.0078, 0.052, -0.449, 0.356, 0.348);
		final SviVolatilitySmile[] smiles = new SviVolatilitySmile[] {smile, };
		final VolatilitySurface surface = new SviVolatilitySurface(valDate, dcc, fwdStructure, smiles, false);

		final LocalDate expiryDate = LocalDate.parse("2020-12-24");
		final double strike = 90.0;

		final double volatility = surface.getVolatility(strike, expiryDate, fwdStructure);

		final boolean[] callOrPut = {true, false};
		final boolean[] americanOrEuropean = {true, false};
		for (final boolean isCall : callOrPut)
		{
			for (final boolean isAmerican : americanOrEuropean)
			{
				Option option;
				if (isAmerican) {
					option = new AmericanOption(expiryDate, strike, isCall);
				} else {
					option = new EuropeanOption(expiryDate, strike, isCall);
				}

				final double pdePrice = pdePricer.getPrice(option, fwdStructure, curve, surface);
				final double pdeLvPrice = pdeLvPricer.getPrice(option, fwdStructure, curve, surface);

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

		final PdeOptionValuation pricer = new PdeOptionValuation(0.1, 3.0, 50, 40, dcc, false, false);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double volatility = 0.25;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.05;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
						new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0),});

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final LocalDate expiryDateBefore = LocalDate.parse("2020-09-16");
		final double strike = 100.0;
		final LocalDate expiryDateAfter = LocalDate.parse("2020-09-17");

		final boolean isCall = true;
		final Option optionBefore = new AmericanOption(expiryDateBefore, strike, isCall);
		final Option optionAfter = new AmericanOption(expiryDateAfter, strike, isCall);

		final double priceBefore = pricer.getPrice(optionBefore, fwdStructure, curve, flatVol);
		final double priceAfter = pricer.getPrice(optionAfter, fwdStructure, curve, flatVol);

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

		final AnalyticOptionValuation anaPricer = new AnalyticOptionValuation(dcc);
		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 5.0, 50, 30, dcc, false, false);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double volatility = 0.35;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.15;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 12.0, 0.02),
						new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.04),});

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final LocalDate expiryDate = LocalDate.parse("2020-06-15");
		final double strike = 100.0;

		final boolean[] callput = {true, false};
		for (final boolean isCall : callput)
		{
			final Option option = new EuropeanOption(expiryDate, strike, isCall);

			final double[] pdeSensis = pdePricer.getPdeSensis(option, fwdStructure, curve, flatVol);
			final double pdeVega = pdePricer.getVega(option, fwdStructure, curve, flatVol, pdeSensis[0], 1e-3);
			final double anaDelta = anaPricer.getDelta(option, fwdStructure, curve, flatVol);
			final double anaGamma = anaPricer.getGamma(option, fwdStructure, curve, flatVol);
			final double anaTheta = anaPricer.getTheta(option, fwdStructure, curve, flatVol);
			final double anaVega = anaPricer.getVega(option, fwdStructure, curve, flatVol);

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
					0.0, pdeSensis[3]/anaTheta - 1.0, 0.04);
		}
	}

	@Test
	public void Test_impliedVol() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test implied volatility");
		System.out.println("========================================");

		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 5.0, 50, 30, dcc, false, false);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double spot = 100.0;
		final double volatility = 0.35;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.05;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 12.0, 0.02),
						new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.04),});

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		final LocalDate expiryDate = LocalDate.parse("2021-06-15");
		final double strike =100.0;

		final boolean[] callput = {true, false};
		for (final boolean isCall : callput)
		{
			final Option option = new AmericanOption(expiryDate, strike, isCall);
			final double price = pdePricer.getPrice(option, fwdStructure, curve, flatVol);
			final double impVol = pdePricer.getImpliedVolatility(option, fwdStructure, curve, price);
			final double priceFromImpVol = pdePricer.getPrice(option, fwdStructure, curve, new FlatVolatilitySurface(impVol));

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

		final PdeOptionValuation pdePricer = new PdeOptionValuation(0.1, 3.0, 70, 0, dcc, false, true);
		final LocalDate valDate = LocalDate.parse("2019-06-15");
		final double volatility = 0.35;
		//double volatility = 0.2;
		final VolatilitySurface flatVol = new FlatVolatilitySurface(volatility);
		final double rate = 0.05;
		//double rate = 0.1;
		final YieldCurve curve = new FlatYieldCurve(valDate, rate, dcc);

		final AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 5.0, 0.00),
						new AffineDividend(LocalDate.parse("2020-09-17"), 3.0, 0.02),
						new AffineDividend(LocalDate.parse("2021-09-17"), 0.0, 0.045),});
		//AffineDividendStream dividends = new AffineDividendStream(new AffineDividend[0]);

		final EquityForwardStructure fwdStructure = new BuehlerDividendForwardStructure(valDate, 100.0, curve, dividends, dcc);

		final LocalDate expiryDate = LocalDate.parse("2020-12-15");
		//LocalDate expiryDate = LocalDate.parse("2019-09-15");
		final double strike = 100.0;

		final boolean[] callput = {true, false};
		final boolean[] american = {true, false};
		final ArrayList<Double> spots = new ArrayList<Double>();
		for (int i = 50; i <= 150; i++) {
			spots.add(1.0 * i);
		}
		//spots = new ArrayList<Double>() {{add(80.0);}};
		System.out.println("Exercise,Type,Spot,Price,Delta,Gamma,Vega,Theta");
		for (final boolean isAmerican : american)
		{
			for (final boolean isCall : callput)
			{
				for (final double spot : spots)
				{
					final EquityForwardStructure thisStructure = fwdStructure.cloneWithNewSpot(spot);
					Option option;
					if(isAmerican) {
						option = new AmericanOption(expiryDate, strike, isCall);
					} else {
						option = new EuropeanOption(expiryDate, strike, isCall);
					}

					final double[] pdeSensis = pdePricer.getPdeSensis(option, thisStructure, curve, flatVol);
					final double pdeVega = pdePricer.getVega(option, thisStructure, curve, flatVol, pdeSensis[0], 1e-6);
					//double pdeTheta = pdePricer.getTheta(option, thisStructure, curve, flatVol, pdeSensis[0]);

					System.out.println((isAmerican ? "American" : "European") + "," + (isCall ? "Call" : "Put") + "," + spot +"," + pdeSensis[0] + "," + pdeSensis[1] + "," + pdeSensis[2] + "," + pdeVega + "," + pdeSensis[3]);
				}
			}
		}
		System.out.println();
	}
}
