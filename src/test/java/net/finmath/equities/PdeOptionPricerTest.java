package net.finmath.equities;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;

import static org.junit.Assert.*;
import org.junit.Test;
import net.finmath.exception.CalculationException;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConventionFactory;
import net.finmath.equities.marketdata.*;
import net.finmath.equities.models.*;
import net.finmath.equities.products.*;
import net.finmath.equities.pricer.*;

/**
 * Tests for the PDE option pricer under a lognormal model with Buehler dividends.
 *
 * @author Andreas Grotz
 */

public class PdeOptionPricerTest {
	/*
	 */
	static final DecimalFormat decform = new DecimalFormat("#0.00");
	DayCountConvention dcc = DayCountConventionFactory.getDayCountConvention("act/365") ;

	@Test
	public void Test_pricer_european() throws CalculationException
	{
		System.out.println("PdeOptionPricer: Test European option price");
		System.out.println("===========================================");

		var anaPricer = new AnalyticOptionPricer(dcc);
		var pdePricer = new PdeOptionPricer(0.1, 5.0, 50, 30, dcc, false, false);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.25;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.05;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 12.0, 0.03),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var expiryDate = LocalDate.parse("2020-12-24");
		var strike = 90.0;

		boolean[] callput = {true, false};
		for (var isCall : callput)
		{
			var option = new EuropeanOption(expiryDate, strike, isCall);

			var anaPrice = anaPricer.getPrice(option, fwdStructure, curve, flatVol);
			var pdePrice = pdePricer.getPrice(option, fwdStructure, curve, flatVol);

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

		var anaPricer = new AnalyticOptionPricer(dcc);
		var pdePricer = new PdeOptionPricer(0.1, 3.0, 50, 30, dcc, false, true);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.25;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.05;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 5.0, 0.00),
						new AffineDividend(LocalDate.parse("2020-09-17"), 3.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 0.0, 0.05),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var expiryDate = LocalDate.parse("2021-10-15");
		var strike = 100.0;

		boolean[] callput = {true, false};
		for (var isCall : callput)
		{
			var americanOption = new AmericanOption(expiryDate, strike, isCall);
			var europeanOption = new EuropeanOption(expiryDate, strike, isCall);

			var anaPrice = anaPricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
			var pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
			var pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

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

		var pdePricer = new PdeOptionPricer(0.1, 3.0, 50, 30, dcc, false, false);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.0000001;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.0;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-06-15"), 10.0, 0.00),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var expiryDate = LocalDate.parse("2021-06-15");
		var strike = 90.0;

		var americanOption = new AmericanOption(expiryDate, strike, true);
		var europeanOption = new EuropeanOption(expiryDate, strike, true);
		var pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
		var pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

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

		var pdePricer = new PdeOptionPricer(0.1, 3.0, 50, 30, dcc, false, false);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 50.0;
		var volatility = 0.5;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.25;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[0]);

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var expiryDate = LocalDate.parse("2021-06-15");
		var strike = 100.0;

		var americanOption = new AmericanOption(expiryDate, strike, false);
		var europeanOption = new EuropeanOption(expiryDate, strike, false);
		var pdePriceEu = pdePricer.getPrice(europeanOption, fwdStructure, curve, flatVol);
		var pdePriceAm = pdePricer.getPrice(americanOption, fwdStructure, curve, flatVol);

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

		var pdePricer = new PdeOptionPricer(0.1, 3.0, 75, 50, dcc, false, false);
		var pdeLvPricer = new PdeOptionPricer(0.1, 3.0, 75, 50, dcc, true, false);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var rate = 0.05;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.03),
						new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.03),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var smile = new SviVolatilitySmile(LocalDate.parse("2021-06-15"), 0.0078, 0.052, -0.449, 0.356, 0.348);
		var smiles = new SviVolatilitySmile[] {smile, };
		var surface = new SviVolatilitySurface(valDate, dcc, fwdStructure, smiles, false);

		var expiryDate = LocalDate.parse("2020-12-24");
		var strike = 90.0;

		var volatility = surface.getVolatility(strike, expiryDate, fwdStructure);

		boolean[] callOrPut = {true, false};
		boolean[] americanOrEuropean = {true, false};
		for (var isCall : callOrPut)
		{
			for (var isAmerican : americanOrEuropean)
			{
				IOption option;
				if (isAmerican)
					option = new AmericanOption(expiryDate, strike, isCall);
				else
					option = new EuropeanOption(expiryDate, strike, isCall);

				var pdePrice = pdePricer.getPrice(option, fwdStructure, curve, surface);
				var pdeLvPrice = pdeLvPricer.getPrice(option, fwdStructure, curve, surface);

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

		var pricer = new PdeOptionPricer(0.1, 3.0, 50, 40, dcc, false, false);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.25;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.05;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
						new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var expiryDateBefore = LocalDate.parse("2020-09-16");
		var strike = 100.0;
		var expiryDateAfter = LocalDate.parse("2020-09-17");

		boolean isCall = true;
		var optionBefore = new AmericanOption(expiryDateBefore, strike, isCall);
		var optionAfter = new AmericanOption(expiryDateAfter, strike, isCall);

		var priceBefore = pricer.getPrice(optionBefore, fwdStructure, curve, flatVol);
		var priceAfter = pricer.getPrice(optionAfter, fwdStructure, curve, flatVol);

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

		var anaPricer = new AnalyticOptionPricer(dcc);
		var pdePricer = new PdeOptionPricer(0.1, 5.0, 50, 30, dcc, false, false);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.35;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.15;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 12.0, 0.02),
						new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.04),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var expiryDate = LocalDate.parse("2020-06-15");
		var strike = 100.0;

		boolean[] callput = {true, false};
		for (var isCall : callput)
		{
			var option = new EuropeanOption(expiryDate, strike, isCall);

			var pdeSensis = pdePricer.getPdeSensis(option, fwdStructure, curve, flatVol);
			var pdeVega = pdePricer.getVega(option, fwdStructure, curve, flatVol, pdeSensis[0], 1e-3);
			var anaDelta = anaPricer.getDelta(option, fwdStructure, curve, flatVol);
			var anaGamma = anaPricer.getGamma(option, fwdStructure, curve, flatVol);
			var anaTheta = anaPricer.getTheta(option, fwdStructure, curve, flatVol);
			var anaVega = anaPricer.getVega(option, fwdStructure, curve, flatVol);

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

		var pdePricer = new PdeOptionPricer(0.1, 5.0, 50, 30, dcc, false, false);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.35;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.05;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 12.0, 0.02),
						new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.04),});

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

		var expiryDate = LocalDate.parse("2021-06-15");
		var strike =100.0;

		boolean[] callput = {true, false};
		for (var isCall : callput)
		{
			var option = new AmericanOption(expiryDate, strike, isCall);
			var price = pdePricer.getPrice(option, fwdStructure, curve, flatVol);
			var impVol = pdePricer.getImpliedVolatility(option, fwdStructure, curve, price);
			var priceFromImpVol = pdePricer.getPrice(option, fwdStructure, curve, new FlatVolatilitySurface(impVol));

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

		var pdePricer = new PdeOptionPricer(0.1, 3.0, 70, 0, dcc, false, true);
		var valDate = LocalDate.parse("2019-06-15");
		var volatility = 0.35;
		//var volatility = 0.2;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.05;
		//var rate = 0.1;
		var curve = new FlatYieldCurve(valDate, rate, dcc);

		var dividends = new AffineDividendStream(new AffineDividend[]
				{new AffineDividend(LocalDate.parse("2019-09-17"), 5.0, 0.00),
						new AffineDividend(LocalDate.parse("2020-09-17"), 3.0, 0.02),
						new AffineDividend(LocalDate.parse("2021-09-17"), 0.0, 0.045),});
		//var dividends = new AffineDividendStream(new AffineDividend[0]);

		var fwdStructure = new BuehlerDividendForwardStructure(valDate, 100.0, curve, dividends, dcc);

		var expiryDate = LocalDate.parse("2020-12-15");
		//var expiryDate = LocalDate.parse("2019-09-15");
		var strike = 100.0;

		boolean[] callput = {true, false};
		boolean[] american = {true, false};
		var spots = new ArrayList<Double>();
		for (int i = 50; i <= 150; i++)
			spots.add(1.0 * i);
		//spots = new ArrayList<Double>() {{add(80.0);}};
		System.out.println("Exercise,Type,Spot,Price,Delta,Gamma,Vega,Theta");
		for (var isAmerican : american)
		{
			for (var isCall : callput)
			{
				for (var spot : spots)
				{
					var thisStructure = fwdStructure.cloneWithNewSpot(spot);
					IOption option;
					if(isAmerican)
						option = new AmericanOption(expiryDate, strike, isCall);
					else
						option = new EuropeanOption(expiryDate, strike, isCall);

					var pdeSensis = pdePricer.getPdeSensis(option, thisStructure, curve, flatVol);
					var pdeVega = pdePricer.getVega(option, thisStructure, curve, flatVol, pdeSensis[0], 1e-6);
					//var pdeTheta = pdePricer.getTheta(option, thisStructure, curve, flatVol, pdeSensis[0]);

					System.out.println((isAmerican ? "American" : "European") + "," + (isCall ? "Call" : "Put") + "," + spot +"," + pdeSensis[0] + "," + pdeSensis[1] + "," + pdeSensis[2] + "," + pdeVega + "," + pdeSensis[3]);
				}
			}
		}
		System.out.println();
	}
}