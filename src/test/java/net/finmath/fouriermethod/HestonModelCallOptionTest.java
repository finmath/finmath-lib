/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 24.03.2014
 */

package net.finmath.fouriermethod;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import net.finmath.analytic.model.curves.DiscountCurve;
import net.finmath.analytic.model.curves.Curve.ExtrapolationMethod;
import net.finmath.analytic.model.curves.Curve.InterpolationEntity;
import net.finmath.analytic.model.curves.Curve.InterpolationMethod;
import net.finmath.exception.CalculationException;
import net.finmath.modelling.exponentialsemimartingales.BlackScholesModel;
import net.finmath.modelling.exponentialsemimartingales.HestonModel;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.fouriermethod.products.AbstractProductFourierTransform;
import net.finmath.fouriermethod.products.DigitalOption;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.montecarlo.RandomVariable;

/**
 * Test class for the valuation of a call option under Heston
 * model using Fourier transforms / characteristic functions.
 * 
 * @author Christian Fries
 */
@RunWith(Parameterized.class)
public class HestonModelCallOptionTest {

	/**
	 * The parameters for this test
	 * 
	 * @return Array of parameters.
	 */
	@Parameters(name="{0}")
	public static Collection<Object[]> generateData()
	{
		return Arrays.asList(new Object[][] {
			{ new EuropeanOption(maturity, strike, discountCurve) },
			{ new DigitalOption(maturity, strike, discountCurve) },
		});
	};

	private final AbstractProductFourierTransform product;
	
	// Model properties
	private final double	initialValue   = 1.0;
	private static final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.30;

	private final double theta = volatility*volatility;
	private final double kappa = 0.1;
	private double xi = 0.50;
	private final double rho = 0.1;

	private static final double maturity	= 1.0;
	private static final double strike		= 0.95;
		
	private static final double[] times = {0.0, 1.0};
	private static final RandomVariable[] discountFactors = {new RandomVariable(1.0), new RandomVariable(Math.exp(-riskFreeRate * maturity))};
	private static final boolean[] isParameter = {false, false};
	private static final DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("discountCurve", times, discountFactors, isParameter,
			InterpolationMethod.PIECEWISE_CONSTANT, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE_PER_TIME);



	public HestonModelCallOptionTest(AbstractProductFourierTransform product) {
		super();
		this.product = product;
	}

	@Test
	public void test() throws CalculationException {

		// For xi close to zero we should recover the Black Scholes model
		double xi = 0.000001;

		ProcessCharacteristicFunctionInterface modelBS = new BlackScholesModel(initialValue, riskFreeRate, volatility);
		ProcessCharacteristicFunctionInterface modelHS = new HestonModel(initialValue, riskFreeRate, volatility, riskFreeRate, theta, kappa, xi, rho);

		long startMillis	= System.currentTimeMillis();

		double valueBS			= product.getValue(modelBS);
		double valueHS			= product.getValue(modelHS);

		long endMillis		= System.currentTimeMillis();

		double error			= valueHS-valueBS;

		System.out.println(product.getClass().getSimpleName() + "\t" + "Result: " + valueHS + ". \tError: " + error + "." + ". \tCalculation time: " + ((endMillis-startMillis)/1000.0) + " sec.");
		Assert.assertEquals("Value", valueBS, valueHS, 1E-7);
	}
}
