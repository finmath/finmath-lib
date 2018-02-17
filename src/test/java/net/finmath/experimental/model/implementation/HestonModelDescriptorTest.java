/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 17.02.2018
 */

package net.finmath.experimental.model.implementation;

import java.time.LocalDate;
import java.util.Map;

import org.junit.Test;

import net.finmath.experimental.model.Model;
import net.finmath.experimental.model.Product;
import net.finmath.experimental.model.ProductDescriptor;
import net.finmath.marketdata.model.curves.Curve.ExtrapolationMethod;
import net.finmath.marketdata.model.curves.Curve.InterpolationEntity;
import net.finmath.marketdata.model.curves.Curve.InterpolationMethod;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;

/**
 * @author Christian Fries
 *
 */
public class HestonModelDescriptorTest {

	// Model properties
	private final double initialValue   = 1.0;
	private final double riskFreeRate   = 0.05;
	private final double volatility     = 0.30;

	private final double theta = volatility*volatility;
	private final double kappa = 0.1;
	private final double xi = 0.50;
	private final double rho = 0.1;

	private static final double maturity = 1.0;
	private static final double strike		= 0.95;

	@Test
	public void test() {
		/*
		 * Create Heston Model descriptor
		 */
		LocalDate referenceDate = LocalDate.of(2017,8,15);
		double[] times = new double[] { 1.0 };
		double[] givenAnnualizedZeroRates = new double[] { riskFreeRate };
		InterpolationMethod interpolationMethod = InterpolationMethod.LINEAR;
		InterpolationEntity interpolationEntity = InterpolationEntity.LOG_OF_VALUE_PER_TIME;
		ExtrapolationMethod extrapolationMethod = ExtrapolationMethod.CONSTANT;
		DiscountCurveInterface discountCurve = DiscountCurve.createDiscountCurveFromAnnualizedZeroRates("discount curve", referenceDate, times, givenAnnualizedZeroRates, interpolationMethod, extrapolationMethod, interpolationEntity);
		
		DiscountCurveInterface discountCurveForForwardRate = discountCurve;
		DiscountCurveInterface discountCurveForDiscountRate = discountCurve;
		HestonModelDescriptor hestonModelDescriptor = new HestonModelDescriptor(referenceDate, initialValue, discountCurveForForwardRate, discountCurveForDiscountRate, volatility, theta, kappa, xi, rho);

		/*
		 * Create European option descriptor
		 */
		String underlyingName = "eurostoxx";
		ProductDescriptor europeanOptionDescriptor = (new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturity, strike));

		/*
		 * Create Fourier implementation of Heston model
		 */
		Model<?> hestonModel = (new HestonModelFourierFactory()).getModelFromDescription(hestonModelDescriptor);
		
		/*
		 * Create product implementation compatible with Heston model
		 */
		Product<?> europeanOption = hestonModel.getProductFromDesciptor(europeanOptionDescriptor);
		
		/*
		 * Evaluate product
		 */
		Map<String, Object> value = europeanOption.getValue(hestonModel);

		System.out.println(value);
	}

}
