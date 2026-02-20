/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 24.03.2014
 */

package net.finmath.fouriermethod;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.BlackScholesModel;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.fouriermethod.products.DigitalOption;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.fouriermethod.products.FourierTransformProduct;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.modelling.products.CallOrPut;

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

		final CharacteristicFunctionModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		final FourierTransformProduct product = new EuropeanOption(maturity, strike);

		final long startMillis	= System.currentTimeMillis();

		final double value			= product.getValue(model);

		final long endMillis		= System.currentTimeMillis();

		final double valueAnalytic	= AnalyticFormulas.blackScholesOptionValue(initialValue, riskFreeRate, volatility, maturity, strike);
		final double error			= value-valueAnalytic;

		System.out.println(product.getClass().getSimpleName() + "\t" + "Result: " + value + ". \tError: " + error + "." + ". \tCalculation time: " + ((endMillis-startMillis)/1000.0) + " sec.");

		Assert.assertEquals("Value", valueAnalytic, value, 1E-7);
	}

	@Test
	public void testPutCallParity() throws CalculationException {

		final CharacteristicFunctionModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		final FourierTransformProduct productCall = new EuropeanOption(maturity, strike);
		final FourierTransformProduct productPut = new EuropeanOption(maturity, strike, CallOrPut.PUT);


		final double valueCall			= productCall.getValue(model);
		final double valuePut			= productPut.getValue(model);


		final double rightHandSide	= valueCall - valuePut;

		final double leftHandSide	= initialValue - strike * Math.exp(-riskFreeRate * maturity);


		Assert.assertEquals("Value", rightHandSide, leftHandSide, 1E-7);
	}

	@Test
	public void testDigitalOption() throws CalculationException {

		final CharacteristicFunctionModel model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		final FourierTransformProduct product = new DigitalOption(maturity, strike);

		final long startMillis	= System.currentTimeMillis();

		final double value			= product.getValue(model);

		final long endMillis		= System.currentTimeMillis();

		final double valueAnalytic	= AnalyticFormulas.blackScholesDigitalOptionValue(initialValue, riskFreeRate, volatility, maturity, strike);
		final double error			= value-valueAnalytic;

		System.out.println(product.getClass().getSimpleName() + "\t" + "Result: " + value + ". \tError: " + error + "." + ". \tCalculation time: " + ((endMillis-startMillis)/1000.0) + " sec.");

		Assert.assertEquals("Value", valueAnalytic, value, 1E-7);
	}
}
