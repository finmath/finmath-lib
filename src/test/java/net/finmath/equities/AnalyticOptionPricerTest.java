package net.finmath.equities;

import java.text.DecimalFormat;
import java.time.LocalDate;

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
 * Tests for the analytic option pricer under a Black-Scholes process with Buehler dividends.
 *
 * @author Andreas Grotz
 */

public class AnalyticOptionPricerTest {
	/*
	*/
	static final DecimalFormat decform = new DecimalFormat("#0.00");
	DayCountConvention dcc = DayCountConventionFactory.getDayCountConvention("act/365") ;
	
	@Test
	public void Test_noArbitrage() throws CalculationException
    {
		System.out.println("AnalyticOptionPricer: Test for arbitrage");
		System.out.println("========================================");
		
		var pricer = new AnalyticOptionPricer(dcc);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.25;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.01;
		var curve = new FlatYieldCurve(valDate, rate, dcc);
		
		var dividends = new AffineDividendStream(new AffineDividend[] 
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
				 new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0),});
		
		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);
		
		var expiryDateBefore = LocalDate.parse("2020-09-16");
		var strikeBefore = 100.0;
		var expiryDateAfter = LocalDate.parse("2020-09-17");
		var strikeAfter = 90.0;
		
		boolean[] callput = {true, false};
		for (var isCall : callput)
		{
			var optionBefore = new EuropeanOption(expiryDateBefore, strikeBefore, isCall);
			var optionAfter = new EuropeanOption(expiryDateAfter, strikeAfter, isCall);

			var priceBefore = pricer.getPrice(optionBefore, fwdStructure, curve, flatVol);
			var priceAfter = pricer.getPrice(optionAfter, fwdStructure, curve, flatVol);
			
			
			var volBefore = pricer.getImpliedVolatility(optionBefore, fwdStructure, curve, priceBefore);
			var volAfter = pricer.getImpliedVolatility(optionAfter, fwdStructure, curve, priceAfter);
			
			//System.out.println("BS Price " + (isCall ? "Call" : "Put") + " before: " + bsPrice);
			System.out.println("Price before: " + priceBefore);
			System.out.println("Price after: " + priceAfter);
			System.out.println("Implied vol before: " + volBefore);
			System.out.println("Implied vol after: " + volAfter);
			System.out.println();
			
			assertEquals("Price before and after dividend should be almost equal",
					0.0, priceAfter/priceBefore - 1.0, 0.005);
			assertEquals("Implied vol before dividend deviates from input vol",
					0.0, volBefore/volatility -1.0, 1E-14);
			assertEquals("Implied vol after dividend deviates from input vol",
					0.0, volAfter/volatility -1.0, 1E-14);
		}		
    }
	
	@Test
	public void Test_sensis() throws CalculationException
    {
		System.out.println("AnalyticOptionPricer: Test Greeks");
		System.out.println("=================================");
		
		var pricer = new AnalyticOptionPricer(dcc);
		var valDate = LocalDate.parse("2019-06-15");
		var spot = 100.0;
		var volatility = 0.35;
		var flatVol = new FlatVolatilitySurface(volatility);
		var rate = 0.15;
		var discountCurve = new FlatYieldCurve(valDate, rate, dcc);
		var repoRate = 0.25;
		var repoCurve = new FlatYieldCurve(valDate, repoRate, dcc);
		
		var dividends = new AffineDividendStream(new AffineDividend[] 
				{new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
				 new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0),});
		
		var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, repoCurve, dividends, dcc);
		
		var expiryDate = LocalDate.parse("2020-12-15");
		var strike = 90.0;
		
		boolean[] callput = {true, false};
		for (var isCall : callput)
		{
			var option = new EuropeanOption(expiryDate, strike, isCall);

			var price = pricer.getPrice(option, fwdStructure, discountCurve, flatVol);
			var spotStep = spot * 0.01;
			var priceUp = pricer.getPrice(option, fwdStructure.cloneWithNewSpot(spot + spotStep), discountCurve, flatVol);
			var priceDown = pricer.getPrice(option, fwdStructure.cloneWithNewSpot(spot - spotStep), discountCurve, flatVol);
			
			var anaDelta = pricer.getDelta(option, fwdStructure, discountCurve, flatVol);
			var anaGamma = pricer.getGamma(option, fwdStructure, discountCurve, flatVol);
			var fdDelta = 0.5 * (priceUp - priceDown) / spotStep;
			var fdGamma = (priceUp + priceDown - 2 * price) / spotStep / spotStep;
			
			var volStep = 0.0001;
			var priceVega = pricer.getPrice(option, fwdStructure, discountCurve, flatVol.getShiftedSurface(volStep));
			var anaVega = pricer.getVega(option, fwdStructure, discountCurve, flatVol);
			var fdVega = (priceVega - price) / volStep;
			
			var anaTheta = pricer.getTheta(option, fwdStructure, discountCurve, flatVol);
			var thetaDate = valDate.plusDays(1);
			var thetaSpot = fwdStructure.getForward(thetaDate);
			var shiftedFwdStructure = fwdStructure.cloneWithNewSpot(thetaSpot).cloneWithNewDate(thetaDate);
			var priceTheta = pricer.getPrice(option, shiftedFwdStructure, discountCurve, flatVol);
			var fdTheta = (priceTheta - price) / dcc.getDaycountFraction(valDate, thetaDate);
			
			
			System.out.println("Ana "+ (isCall ? "Call" : "Put") + " Delta: " + anaDelta);
			System.out.println("FinDiff  "+ (isCall ? "Call" : "Put") + " Delta: " + fdDelta);
			System.out.println("Ana " + (isCall ? "Call" : "Put") + " Gamma: " + anaGamma);
			System.out.println("FinDiff " + (isCall ? "Call" : "Put") + " Gamma: " + fdGamma);
			System.out.println("Ana " + (isCall ? "Call" : "Put") + " Vega: " + anaVega);
			System.out.println("FinDiff " + (isCall ? "Call" : "Put") + " Vega: " + fdVega);
			System.out.println("Ana " + (isCall ? "Call" : "Put") + " Theta: " + anaTheta);
			System.out.println("FinDiff " + (isCall ? "Call" : "Put") + " Theta: " + fdTheta);
			System.out.println();
			
			assertEquals("Analytic Delta formula and finite difference approximation deviate too much.",
					0.0, anaDelta/fdDelta -1.0, 0.01);
			assertEquals("Analytic Gamma formula and finite difference approximation deviate too much",
					0.0, anaGamma/fdGamma -1.0, 0.01);
			assertEquals("Analytic Vega formula and finite difference approximation deviate too much",
					0.0, anaVega/fdVega -1.0, 0.01);
			assertEquals("Analytic Theta formula and finite difference approximation deviate too much",
					0.0, anaTheta/fdTheta -1.0, 0.01);
		}		
    }
}