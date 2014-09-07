/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 30.06.2014
 */

package net.finmath.marketdata.model.volatilities;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterface;
import net.finmath.marketdata.model.volatilities.VolatilitySurfaceInterface.QuotingConvention;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test of the caplet volatilities class.
 * 
 * @author Christian Fries
 */
public class CapletVolatilitiesTest {

	@Test
	public void testConversions() {

		double[] times			= { 0.0, 1.0, 2.0, 10.0 };
		double[] givenForwards	= { 0.02, 0.02, 0.02, 0.02 };
		double paymentOffset	= 0.25;
		ForwardCurveInterface forwardCurve = ForwardCurve.createForwardCurveFromForwards("EUR 3M", times, givenForwards, paymentOffset);

		double[] maturities		= { 0.0, 1.0, 2.0, 10.0 };
		double[] strikes		= { 0.02, 0.02, 0.02, 0.02 };
		double[] volatilities	= { 0.2, 0.2, 0.2, 0.2 };
		VolatilitySurfaceInterface.QuotingConvention volatilityConvention = QuotingConvention.VOLATILITYLOGNORMAL;

		double[] timesDf				= { 0.0, 1.0, 2.0, 10.0 };
		double[] givenDiscountFactors	= { 1.0, 0.98, 0.96, 0.90 };
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("EUR OIS", timesDf, givenDiscountFactors);

		AbstractVolatilitySurface capletVolatilities = new CapletVolatilities("EUR Caplet", null, forwardCurve, maturities, strikes, volatilities, volatilityConvention, discountCurve);

		double optionMaturity	= 1.0;
		double[] optionStrikes	= { 0.001, 0.005, 0.010, 0.015, 0.018, 0.020, 0.022, 0.050, 0.10 };
		double[] volLogNormals	= { 0.01, 0.05, 0.10, 0.15, 0.20, 0.25, 0.50, 1.00 };
		for(double volLogNormal : volLogNormals) {
			for(double optionStrike : optionStrikes) {
				System.out.println("Testing:\t Stike = " + optionStrike + "\tImplied Volatility = " + volLogNormal);

				// Testing conversion
				double price1			= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volLogNormal, QuotingConvention.VOLATILITYLOGNORMAL, QuotingConvention.PRICE);
				double volLogNormal1	= capletVolatilities.convertFromTo(optionMaturity, optionStrike, price1, QuotingConvention.PRICE, QuotingConvention.VOLATILITYLOGNORMAL);
				double volNormal1		= capletVolatilities.convertFromTo(optionMaturity, optionStrike, price1, QuotingConvention.PRICE, QuotingConvention.VOLATILITYNORMAL);
				double volNormal2		= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volLogNormal, QuotingConvention.VOLATILITYLOGNORMAL, QuotingConvention.VOLATILITYNORMAL);
				double voLogNormal2		= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volNormal2, QuotingConvention.VOLATILITYNORMAL, QuotingConvention.VOLATILITYLOGNORMAL);
				double price2			= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volNormal1, QuotingConvention.VOLATILITYNORMAL, QuotingConvention.PRICE);

				Assert.assertTrue(Math.abs(price1 - price2) / (1+Math.abs(price1 - price2)) < 1E-10);
				Assert.assertTrue(Math.abs(volLogNormal1 - voLogNormal2) / (1+Math.abs(volLogNormal1 - voLogNormal2)) < 1E-10);
				Assert.assertTrue(Math.abs(volNormal1 - volNormal2) / (1+Math.abs(volNormal1 - volNormal2)) < 1E-10);
			}
		}
	}
}
