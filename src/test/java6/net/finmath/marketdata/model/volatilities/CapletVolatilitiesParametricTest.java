/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.09.2014
 */

package net.finmath.marketdata.model.volatilities;

import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;

import org.junit.Test;

/**
 * @author Christian Fries
 *
 */
public class CapletVolatilitiesParametricTest {

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
}
