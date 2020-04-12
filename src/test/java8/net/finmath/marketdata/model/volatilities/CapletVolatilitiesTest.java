/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.06.2014
 */

package net.finmath.marketdata.model.volatilities;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurveInterpolation;
import net.finmath.marketdata.model.volatilities.VolatilitySurface.QuotingConvention;

/**
 * Test of the caplet volatilities class.
 *
 * @author Christian Fries
 */
public class CapletVolatilitiesTest {

	@Test
	public void testConversions() {

		final double[] times			= { 0.0, 1.0, 2.0, 10.0 };
		final double[] givenForwards	= { 0.02, 0.02, 0.02, 0.02 };
		final double paymentOffset	= 0.25;
		final ForwardCurve forwardCurve = ForwardCurveInterpolation.createForwardCurveFromForwards("EUR 3M", times, givenForwards, paymentOffset);

		final double[] maturities		= { 0.0, 1.0, 2.0, 10.0 };
		final double[] strikes		= { 0.02, 0.02, 0.02, 0.02 };
		final double[] volatilities	= { 0.2, 0.2, 0.2, 0.2 };
		final VolatilitySurface.QuotingConvention volatilityConvention = QuotingConvention.VOLATILITYLOGNORMAL;

		final double[] timesDf				= { 0.0, 1.0, 2.0, 10.0 };
		final double[] givenDiscountFactors	= { 1.0, 0.98, 0.96, 0.90 };
		final DiscountCurve discountCurve = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors("EUR OIS", timesDf, givenDiscountFactors);

		final AbstractVolatilitySurface capletVolatilities = new CapletVolatilities("EUR Caplet", null, forwardCurve, maturities, strikes, volatilities, volatilityConvention, discountCurve);

		final double optionMaturity	= 1.0;
		final double[] optionStrikes	= { 0.001, 0.005, 0.010, 0.015, 0.018, 0.020, 0.022, 0.050, 0.10 };
		final double[] volLogNormals	= { 0.01, 0.05, 0.10, 0.15, 0.20, 0.25, 0.50, 1.00 };
		for(final double volLogNormal : volLogNormals) {
			for(final double optionStrike : optionStrikes) {
				System.out.println("Testing:\t Stike = " + optionStrike + "\tImplied Volatility = " + volLogNormal);

				// Testing conversion
				final double price1			= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volLogNormal, QuotingConvention.VOLATILITYLOGNORMAL, QuotingConvention.PRICE);
				final double volLogNormal1	= capletVolatilities.convertFromTo(optionMaturity, optionStrike, price1, QuotingConvention.PRICE, QuotingConvention.VOLATILITYLOGNORMAL);
				final double volNormal1		= capletVolatilities.convertFromTo(optionMaturity, optionStrike, price1, QuotingConvention.PRICE, QuotingConvention.VOLATILITYNORMAL);
				final double volNormal2		= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volLogNormal, QuotingConvention.VOLATILITYLOGNORMAL, QuotingConvention.VOLATILITYNORMAL);
				final double voLogNormal2		= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volNormal2, QuotingConvention.VOLATILITYNORMAL, QuotingConvention.VOLATILITYLOGNORMAL);
				final double price2			= capletVolatilities.convertFromTo(optionMaturity, optionStrike, volNormal1, QuotingConvention.VOLATILITYNORMAL, QuotingConvention.PRICE);

				Assert.assertEquals("Price deviation", 0.0, (price1 - price2) / (1+Math.abs(price1 - price2)), 1E-10);
				Assert.assertEquals("Implied log-normal volatility deviation", 0.0, (volLogNormal1 - voLogNormal2) / (1+Math.abs(volLogNormal1 - voLogNormal2)), 1E-10);
				Assert.assertEquals("Implied normal volatility deviation", 0.0, (volNormal1 - volNormal2) / (1+Math.abs(volNormal1 - volNormal2)), 1E-10);
			}
		}
	}
}
