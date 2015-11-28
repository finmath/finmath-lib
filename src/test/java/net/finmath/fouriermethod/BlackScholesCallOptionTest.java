/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 24.03.2014
 */

package net.finmath.fouriermethod;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.BlackScholesModel;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.fouriermethod.products.AbstractProductFourierTransform;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.functions.AnalyticFormulas;

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

	@Test
	public void test() throws CalculationException {

		ProcessCharacteristicFunctionInterface model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		AbstractProductFourierTransform product = new EuropeanOption(maturity, strike);

		long startMillis	= System.currentTimeMillis();
		
		double value			= product.getValue(model);
		
		long endMillis		= System.currentTimeMillis();
		
		double valueAnalytic	= AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, maturity, strike);
		double error			= value-valueAnalytic;

		System.out.println("Result: " + value + ". \tError: " + error + "." + ". \tCalculation time: " + ((endMillis-startMillis)/1000.0) + " sec.");

		Assert.assertEquals("Value", valueAnalytic, value, 1E-7);
	}
}
