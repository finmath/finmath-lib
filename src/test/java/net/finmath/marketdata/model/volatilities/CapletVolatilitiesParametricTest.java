/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 04.09.2014
 */

package net.finmath.marketdata.model.volatilities;

import static org.junit.Assert.assertTrue;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;

/**
 * @author Christian Fries
 *
 */
public class CapletVolatilitiesParametricTest {

	private static DecimalFormat formatterMaturity	= new DecimalFormat("00.00", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterValue		= new DecimalFormat("  ##0.000%;-##0.000%", new DecimalFormatSymbols(Locale.ENGLISH));
	private static DecimalFormat formatterDeviation	= new DecimalFormat(" 0.00000E00;-0.00000E00", new DecimalFormatSymbols(Locale.ENGLISH));

	@Test
	public void testFlatVolatilityUsingD() {
		LocalDate referenceDate = LocalDate.now();
		double a = 0, b = 0, c = 0, d = 0.4;
		CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		double maturity	= 2.0;
		double strike	= 0.03;
		double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - d < 1E-15);
	}

	@Test
	public void testFlatVolatilityUsingA() {
		LocalDate referenceDate = LocalDate.now();
		double a = 0.4, b = 0, c = 0, d = 0.0;
		CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		double maturity	= 2.0;
		double strike	= 0.03;
		double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - a < 1E-15);
	}

	@Test
	public void testDecayVolatility() {
		LocalDate referenceDate = LocalDate.now();
		double a = 0.1, b = 0, c = 0.2, d = 0.0;
		CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		double maturity	= 2.0;
		double strike	= 0.03;
		double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - a * Math.sqrt((1-Math.exp(- 2 * c * maturity)) / (2 * c * maturity)) < 1E-15);
	}

	@Test
	public void testIntegratedFourParameterExponentialVolatilityParamSet1() {
		double a = 0.25;
		double b = 3.00;
		double c = 1.50;
		double d = 0.10;

		testIntegratedFourParameterExponentialVolatility(a, b, c, d);
	}

	@Test
	public void testIntegratedFourParameterExponentialVolatilityParamSetCZero() {
		double a = 0.25;
		double b = 0.02;
		double c = 0.00;
		double d = 0.10;

		testIntegratedFourParameterExponentialVolatility(a, b, c, d);
	}

	private void testIntegratedFourParameterExponentialVolatility(double a, double b, double c, double d) {

		final double eps = 1E-5;

		System.out.println("Comparing finite difference differentiation of integrated variance with analytic value:\n");
		System.out.println("  t  " + "\t" + " analytic " + "\t" + " finite diff " + "\t" + " deviation ");

		LocalDate referenceDate = LocalDate.now();
		CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("volSurface", referenceDate, a, b, c, d);

		double maxAbsDeviation = 0.0;
		for(double t=0.01; t<=10; t += 0.01) {

			double volatilityUp = volatilityModel.getValue(t+eps, 0.0, QuotingConvention.VOLATILITYLOGNORMAL);
			double volatilityDn = volatilityModel.getValue(t-eps, 0.0, QuotingConvention.VOLATILITYLOGNORMAL);
			double integratedVarianceUp = volatilityUp*volatilityUp*(t+eps);
			double integratedVarianceDn = volatilityDn*volatilityDn*(t-eps);

			double valueFiniteDifference = (integratedVarianceUp-integratedVarianceDn) / (2*eps);
			double sigma = (a + b*t)*Math.exp(-c*t)+d;
			double value = sigma*sigma;
			double deviation = value-valueFiniteDifference;

			System.out.print(formatterMaturity.format(t) + "\t");
			System.out.print(formatterValue.format(value) + "\t");
			System.out.print(formatterValue.format(valueFiniteDifference) + "\t");
			System.out.print(formatterDeviation.format(value-valueFiniteDifference) + "\n");

			maxAbsDeviation = Math.max(maxAbsDeviation, Math.abs(deviation));
		}

		System.out.println("Maximum abs deviation: " + formatterDeviation.format(maxAbsDeviation));
		System.out.println("__________________________________________________________________________________________\n");

		/*
		 * jUnit assertion: condition under which we consider this test successful
		 */
		Assert.assertEquals("Deviation", 0.0, maxAbsDeviation, 1E-8);
	}
}
