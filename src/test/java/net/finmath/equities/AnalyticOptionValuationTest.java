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
import net.finmath.equities.marketdata.YieldCurve;
import net.finmath.equities.models.BuehlerDividendForwardStructure;
import net.finmath.equities.models.FlatVolatilitySurface;
import net.finmath.equities.models.SviVolatilitySurface;
import net.finmath.equities.pricer.AnalyticOptionValuation;
import net.finmath.equities.products.EuropeanOption;
import net.finmath.exception.CalculationException;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConventionFactory;

/**
 * Tests for the analytic option pricer under a Black-Scholes process with Buehler dividends.
 *
 * @author Andreas Grotz
 */

public class AnalyticOptionValuationTest {

    /*
     */
    private static final DecimalFormat decform = new DecimalFormat("#0.00");
    private static final DayCountConvention dcc = DayCountConventionFactory.getDayCountConvention("act/365");

    @Test
    public void Test_noArbitrage() throws CalculationException {
        System.out.println("AnalyticOptionPricer: Test for arbitrage");
        System.out.println("========================================");

        final var pricer = new AnalyticOptionValuation(dcc);
        final var valDate = LocalDate.parse("2019-06-15");
        final var spot = 100.0;
        final var volatility = 0.25;
        final var flatVol = new FlatVolatilitySurface(volatility);
        final var rate = 0.01;
        final var curve = new FlatYieldCurve(valDate, rate, dcc);

        final var dividends = new AffineDividendStream(
                new AffineDividend[] { new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
                        new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0), });

        final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, curve, dividends, dcc);

        final var expiryDateBefore = LocalDate.parse("2020-09-16");
        final var strikeBefore = 100.0;
        final var expiryDateAfter = LocalDate.parse("2020-09-17");
        final var strikeAfter = 90.0;

        final boolean[] callput = { true, false };
        for (final var isCall : callput) {
            final var optionBefore = new EuropeanOption(expiryDateBefore, strikeBefore, isCall);
            final var optionAfter = new EuropeanOption(expiryDateAfter, strikeAfter, isCall);

            final var priceBefore = pricer.getPrice(optionBefore, fwdStructure, curve, flatVol);
            final var priceAfter = pricer.getPrice(optionAfter, fwdStructure, curve, flatVol);

            final var volBefore = pricer.getImpliedVolatility(optionBefore, fwdStructure, curve, priceBefore);
            final var volAfter = pricer.getImpliedVolatility(optionAfter, fwdStructure, curve, priceAfter);

            //System.out.println("BS Price " + (isCall ? "Call" : "Put") + " before: " + bsPrice);
            System.out.println("Price before: " + priceBefore);
            System.out.println("Price after: " + priceAfter);
            System.out.println("Implied vol before: " + volBefore);
            System.out.println("Implied vol after: " + volAfter);
            System.out.println();

            assertEquals("Price before and after dividend should be almost equal", 0.0, priceAfter / priceBefore - 1.0,
                    0.005);
            assertEquals("Implied vol before dividend deviates from input vol", 0.0, volBefore / volatility - 1.0,
                    1E-14);
            assertEquals("Implied vol after dividend deviates from input vol", 0.0, volAfter / volatility - 1.0, 1E-14);
        }
    }

    @Test
    public void Test_sensis() throws CalculationException {
        System.out.println("AnalyticOptionPricer: Test Greeks");
        System.out.println("=================================");

        final var pricer = new AnalyticOptionValuation(dcc);
        final var valDate = LocalDate.parse("2019-06-15");
        final var spot = 100.0;
        final var volatility = 0.35;
        final var flatVol = new FlatVolatilitySurface(volatility);
        final var rate = 0.15;
        final var discountCurve = new FlatYieldCurve(valDate, rate, dcc);
        final var repoRate = 0.25;
        final var repoCurve = new FlatYieldCurve(valDate, repoRate, dcc);

        final var dividends = new AffineDividendStream(
                new AffineDividend[] { new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
                        new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0), });

        final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, repoCurve, dividends, dcc);

        final var expiryDate = LocalDate.parse("2020-12-15");
        final var strike = 90.0;

        final boolean[] callput = { true, false };
        for (final var isCall : callput) {
            final var option = new EuropeanOption(expiryDate, strike, isCall);

            final var price = pricer.getPrice(option, fwdStructure, discountCurve, flatVol);
            final var spotStep = spot * 0.01;
            final var priceUp = pricer.getPrice(option, fwdStructure.cloneWithNewSpot(spot + spotStep), discountCurve,
                    flatVol);
            final var priceDown = pricer.getPrice(option, fwdStructure.cloneWithNewSpot(spot - spotStep), discountCurve,
                    flatVol);

            final var anaDelta = pricer.getDelta(option, fwdStructure, discountCurve, flatVol);
            final var anaGamma = pricer.getGamma(option, fwdStructure, discountCurve, flatVol);
            final var fdDelta = 0.5 * (priceUp - priceDown) / spotStep;
            final var fdGamma = (priceUp + priceDown - 2 * price) / spotStep / spotStep;

            final var volStep = 0.0001;
            final var priceVega = pricer.getPrice(option, fwdStructure, discountCurve,
                    flatVol.getShiftedSurface(volStep));
            final var anaVega = pricer.getVega(option, fwdStructure, discountCurve, flatVol);
            final var fdVega = (priceVega - price) / volStep;

            final var anaTheta = pricer.getTheta(option, fwdStructure, discountCurve, flatVol);
            final var thetaDate = valDate.plusDays(1);
            final var thetaSpot = fwdStructure.getForward(thetaDate);
            final var thetaCurve = discountCurve.rollToDate(thetaDate);
            final var shiftedFwdStructure = fwdStructure.cloneWithNewSpot(thetaSpot).cloneWithNewDate(thetaDate);
            final var priceTheta = pricer.getPrice(option, shiftedFwdStructure, thetaCurve, flatVol);
            final var fdTheta = (priceTheta - price) / dcc.getDaycountFraction(valDate, thetaDate);

            System.out.println("Ana " + (isCall ? "Call" : "Put") + " Delta: " + anaDelta);
            System.out.println("FinDiff  " + (isCall ? "Call" : "Put") + " Delta: " + fdDelta);
            System.out.println("Ana " + (isCall ? "Call" : "Put") + " Gamma: " + anaGamma);
            System.out.println("FinDiff " + (isCall ? "Call" : "Put") + " Gamma: " + fdGamma);
            System.out.println("Ana " + (isCall ? "Call" : "Put") + " Vega: " + anaVega);
            System.out.println("FinDiff " + (isCall ? "Call" : "Put") + " Vega: " + fdVega);
            System.out.println("Ana " + (isCall ? "Call" : "Put") + " Theta: " + anaTheta);
            System.out.println("FinDiff " + (isCall ? "Call" : "Put") + " Theta: " + fdTheta);
            System.out.println();

            assertEquals("Analytic Delta formula and finite difference approximation deviate too much.", 0.0,
                    anaDelta / fdDelta - 1.0, 0.01);
            assertEquals("Analytic Gamma formula and finite difference approximation deviate too much", 0.0,
                    anaGamma / fdGamma - 1.0, 0.01);
            assertEquals("Analytic Vega formula and finite difference approximation deviate too much", 0.0,
                    anaVega / fdVega - 1.0, 0.01);
            assertEquals("Analytic Theta formula and finite difference approximation deviate too much", 0.0,
                    anaTheta / fdTheta - 1.0, 0.01);
        }
    }

    @Test
    public void Test_complexMarketData() throws CalculationException {
        System.out.println("AnalyticOptionPricer: Test with complex market data");
        System.out.println("========================================");

        final var pricer = new AnalyticOptionValuation(dcc);
        final var valDate = LocalDate.parse("2019-06-15");
        final var spot = 100.0;

        LocalDate[] curveDates = new LocalDate[] { LocalDate.parse("2019-09-15"), LocalDate.parse("2019-12-15"),
                LocalDate.parse("2020-03-15"), LocalDate.parse("2020-06-15"), LocalDate.parse("2020-12-15"),
                LocalDate.parse("2021-06-15") };
        final var discountCurve = new YieldCurve("discount", valDate, dcc, curveDates,
                new double[] { 0.991216881, 0.983837517, 0.977731147, 0.970365774, 0.959480762, 0.951164274 });

        final var repoCurve = new YieldCurve("discount", valDate, dcc, curveDates,
                new double[] { 0.988721617, 0.978917197, 0.970418946, 0.960684153, 0.945157112, 0.932304417 });
        final var dividends = new AffineDividendStream(
                new AffineDividend[] { new AffineDividend(LocalDate.parse("2020-09-17"), 10.0, 0.0),
                        new AffineDividend(LocalDate.parse("2021-09-17"), 10.0, 0.0), });
        final var fwdStructure = new BuehlerDividendForwardStructure(valDate, spot, repoCurve, dividends, dcc);

        final var volaPoints = new ArrayList<VolatilityPoint>();
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2019-11-03"), 60, 0.4076));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2019-11-03"), 80, 0.2696));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2019-11-03"), 100, 0.1632));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2019-11-03"), 120, 0.1315));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2019-11-03"), 140, 0.1778));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-05-03"), 60, 0.3282));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-05-03"), 80, 0.2357));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-05-03"), 100, 0.1661));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-05-03"), 120, 0.1220));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-05-03"), 140, 0.1323));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-11-01"), 60, 0.2960));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-11-01"), 80, 0.2253));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-11-01"), 100, 0.1715));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-11-01"), 120, 0.1299));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2020-11-01"), 140, 0.1234));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2021-05-01"), 60, 0.2774));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2021-05-01"), 80, 0.2193));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2021-05-01"), 100, 0.1759));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2021-05-01"), 120, 0.1381));
        volaPoints.add(new VolatilityPoint(LocalDate.parse("2021-05-01"), 140, 0.1264));
        final var volSurface = new SviVolatilitySurface(dcc, false);
        volSurface.calibrate(fwdStructure, volaPoints);

        final var expiryDateBefore = LocalDate.parse("2020-09-16");
        final var strikeBefore = 100.0;
        final var expiryDateAfter = LocalDate.parse("2020-09-17");
        final var strikeAfter = 90.0;

        final boolean[] callput = { true, false };
        for (final var isCall : callput) {
            final var optionBefore = new EuropeanOption(expiryDateBefore, strikeBefore, isCall);
            final var optionAfter = new EuropeanOption(expiryDateAfter, strikeAfter, isCall);

            final var priceBefore = pricer.getPrice(optionBefore, fwdStructure, discountCurve, volSurface);
            final var priceAfter = pricer.getPrice(optionAfter, fwdStructure, discountCurve, volSurface);

            final var volBefore = pricer.getImpliedVolatility(optionBefore, fwdStructure, discountCurve, priceBefore);
            final var volAfter = pricer.getImpliedVolatility(optionAfter, fwdStructure, discountCurve, priceAfter);

            //System.out.println("BS Price " + (isCall ? "Call" : "Put") + " before: " + bsPrice);
            System.out.println("Price before: " + priceBefore);
            System.out.println("Price after: " + priceAfter);
            System.out.println("Implied vol before: " + volBefore);
            System.out.println("Implied vol after: " + volAfter);
            System.out.println();

            assertEquals("Price before and after dividend should be almost equal", 0.0, priceAfter / priceBefore - 1.0,
                    0.005);
        }
    }
}
