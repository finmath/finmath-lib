/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.09.2014
 */

package net.finmath.marketdata.model.volatilities;

import static org.junit.Assert.*;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;

import org.junit.Assert;
import org.junit.Test;

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
		Calendar referenceDate = new GregorianCalendar();
		double a = 0, b = 0, c = 0, d = 0.4;
		CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		double maturity	= 2.0;
		double strike	= 0.03;
		double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - d < 1E-15);
	}

	@Test
	public void testFlatVolatilityUsingA() {
		Calendar referenceDate = new GregorianCalendar();
		double a = 0.4, b = 0, c = 0, d = 0.0;
		CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		double maturity	= 2.0;
		double strike	= 0.03;
		double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);

		assertTrue(volatility - a < 1E-15);
	}

	@Test
	public void testDecayVolatility() {
		Calendar referenceDate = new GregorianCalendar();
		double a = 0.1, b = 0, c = 0.2, d = 0.0;
		CapletVolatilitiesParametric volatilityModel = new CapletVolatilitiesParametric("flat", referenceDate, a, b, c, d);

		double maturity	= 2.0;
		double strike	= 0.03;
		double volatility = volatilityModel.getValue(maturity, strike, QuotingConvention.VOLATILITYLOGNORMAL);
		
		assertTrue(volatility - a * Math.sqrt((1-Math.exp(- 2 * c * maturity)) / (2 * c * maturity)) < 1E-15);
	}
	
	@Test
	public void testIntegratedFourParameterExponentialVolatility() {

		double a = 0.25;
		double b = 3.00;
		double c = 1.50;
		double d = 0.10;

		double eps = 1E-5;

		System.out.println("Comparing finite difference differentiation of integrated variance with analytic value:\n");
		System.out.println("  t  " + "\t" + " analytic " + "\t" + " finite diff " + "\t" + " deviation ");

		Calendar referenceDate = new GregorianCalendar();
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
		Assert.assertTrue(Math.abs(maxAbsDeviation) < 1E-8);
	}
}
