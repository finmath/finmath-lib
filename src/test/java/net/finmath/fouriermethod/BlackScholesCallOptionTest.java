/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 24.03.2014
 */

package net.finmath.fouriermethod;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.analytic.model.curves.Curve.ExtrapolationMethod;
import net.finmath.analytic.model.curves.Curve.InterpolationEntity;
import net.finmath.analytic.model.curves.Curve.InterpolationMethod;
import net.finmath.analytic.model.curves.DiscountCurve;
import net.finmath.exception.CalculationException;
import net.finmath.modelling.exponentialsemimartingales.BlackScholesModel;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.fouriermethod.products.AbstractProductFourierTransform;
import net.finmath.fouriermethod.products.DigitalOption;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.RandomVariable;

/**
 * Test class for the valuation of a call option under Black Scholes
 * model using Fourier transforms / characteristic functions.
 * 
 * @author Christian Fries
 */
public class BlackScholesCallOptionTest {

	private static final double initialValue	= 100.0;
	private static final double volatility		= 0.25;
	private static final double riskFreeRate	= 0.05;

	private static final double maturity	= 1.0;
	private static final double strike		= 95;
	
	private static final double[] times = {0.0, 1.0};
	private static final RandomVariable[] discountFactors = {new RandomVariable(1.0), new RandomVariable(Math.exp(-riskFreeRate * maturity))};
	private static final boolean[] isParameter = {false, false};
	private static final DiscountCurve discountCurve = DiscountCurve.createDiscountCurveFromDiscountFactors("discountCurve", times, discountFactors, isParameter,
			InterpolationMethod.PIECEWISE_CONSTANT, ExtrapolationMethod.CONSTANT, InterpolationEntity.LOG_OF_VALUE_PER_TIME);

	@Test
	public void test() throws CalculationException {

		ProcessCharacteristicFunctionInterface model = new BlackScholesModel(initialValue, riskFreeRate, volatility);
				
		AbstractProductFourierTransform product = new EuropeanOption(maturity, strike,discountCurve);

		long startMillis	= System.currentTimeMillis();
		
		double value			= product.getValue(model);
		
		long endMillis		= System.currentTimeMillis();
		
		double valueAnalytic	= AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, maturity, strike);
		double error			= value-valueAnalytic;

		System.out.println(product.getClass().getSimpleName() + "\t" + "Result: " + value + ". \tError: " + error + "." + ". \tCalculation time: " + ((endMillis-startMillis)/1000.0) + " sec.");
		
		Assert.assertEquals("Value", valueAnalytic, value, 1E-7);
	}

	@Test
	public void testDigitalOption() throws CalculationException {

		ProcessCharacteristicFunctionInterface model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		AbstractProductFourierTransform product = new DigitalOption(maturity, strike, discountCurve);

		long startMillis	= System.currentTimeMillis();
		
		double value			= product.getValue(model);
		
		long endMillis		= System.currentTimeMillis();
		
		double valueAnalytic	= AnalyticFormulas.blackScholesDigitalOptionValue(initialValue, riskFreeRate, volatility, maturity, strike);
		double error			= value-valueAnalytic;

		System.out.println(product.getClass().getSimpleName() + "\t" + "Result: " + value + ". \tError: " + error + "." + ". \tCalculation time: " + ((endMillis-startMillis)/1000.0) + " sec.");

		Assert.assertEquals("Value", valueAnalytic, value, 1E-7);
	}
}
