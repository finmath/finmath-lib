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

import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;

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
		final LocalDate referenceDate = LocalDate.now();
		final double a = 0, b = 0, c = 0, d = 0.4;
		final CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		final double maturity	= 2.0;
		final double strike	= 0.03;
		final double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - d < 1E-15);
	}

	@Test
	public void testFlatVolatilityUsingA() {
		final LocalDate referenceDate = LocalDate.now();
		final double a = 0.4, b = 0, c = 0, d = 0.0;
		final CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		final double maturity	= 2.0;
		final double strike	= 0.03;
		final double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - a < 1E-15);
	}

	@Test
	public void testDecayVolatility() {
		final LocalDate referenceDate = LocalDate.now();
		final double a = 0.1, b = 0, c = 0.2, d = 0.0;
		final CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		final double maturity	= 2.0;
		final double strike	= 0.03;
		final double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - a * Math.sqrt((1-Math.exp(- 2 * c * maturity)) / (2 * c * maturity)) < 1E-15);
	}

	@Test
	public void testIntegratedFourParameterExponentialVolatilityParamSet1() {
		final double a = 0.25;
		final double b = 3.00;
		final double c = 1.50;
		final double d = 0.10;

		testIntegratedFourParameterExponentialVolatility(a, b, c, d);
	}

	@Test
	public void testIntegratedFourParameterExponentialVolatilityParamSetCZero() {
		final double a = 0.25;
		final double b = 0.02;
		final double c = 0.00;
		final double d = 0.10;

		testIntegratedFourParameterExponentialVolatility(a, b, c, d);
	}

	private void testIntegratedFourParameterExponentialVolatility(final double a, final double b, final double c, final double d) {

		final double eps = 1E-5;

		System.out.println("Comparing finite difference differentiation of integrated variance with analytic value:\n");
		System.out.println("  t  " + "\t" + " analytic " + "\t" + " finite diff " + "\t" + " deviation ");

		final LocalDate referenceDate = LocalDate.now();
		final CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("volSurface", referenceDate, a, b, c, d);

		double maxAbsDeviation = 0.0;
		for(double t=0.01; t<=10; t += 0.01) {

			final double volatilityUp = volatilityModel.getValue(t+eps, 0.0, QuotingConvention.VOLATILITYLOGNORMAL);
			final double volatilityDn = volatilityModel.getValue(t-eps, 0.0, QuotingConvention.VOLATILITYLOGNORMAL);
			final double integratedVarianceUp = volatilityUp*volatilityUp*(t+eps);
			final double integratedVarianceDn = volatilityDn*volatilityDn*(t-eps);

			final double valueFiniteDifference = (integratedVarianceUp-integratedVarianceDn) / (2*eps);
			final double sigma = (a + b*t)*Math.exp(-c*t)+d;
			final double value = sigma*sigma;
			final double deviation = value-valueFiniteDifference;

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
