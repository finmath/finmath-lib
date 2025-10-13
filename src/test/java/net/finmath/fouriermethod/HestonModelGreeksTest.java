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
import net.finmath.fouriermethod.models.HestonModel;
import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.fouriermethod.products.FourierTransformProduct;

/**
 * Test class for the delta of a call option under Heston
 * model using Fourier transforms / characteristic functions.
 *
 * @author Christian Fries
 */
public class HestonModelGreeksTest {

	// Model properties
	private final double	initialValue   = 1.0;
	private final double	riskFreeRate   = 0.05;
	private final double	volatility     = 0.20;

	private final double theta = volatility*volatility;
	private final double kappa = 0.8455;
	private final double xi = 0.0001;
	private final double rho = 0.0;

	private final double discountRate = riskFreeRate;

	private static final double maturity	= 3.0;
	private static final double strike		= 0.90;

	public HestonModelGreeksTest() {
	}

	@Test
	public void testBackScholes() throws CalculationException {
		final FourierTransformProduct product = new EuropeanOption(maturity, strike);

		// For xi close to zero we should recover the Black Scholes model
		final double xi = 0.000001;

		final double shift = 1E-05;

		final long startMillis	= System.currentTimeMillis();

		final CharacteristicFunctionModel modelBSUp = new BlackScholesModel(initialValue+shift, riskFreeRate, discountRate, volatility);
		final CharacteristicFunctionModel modelBSDn = new BlackScholesModel(initialValue-shift, riskFreeRate, discountRate, volatility);

		final double valueBSUp			= product.getValue(modelBSUp);
		final double valueBSDn			= product.getValue(modelBSDn);
		final double deltaBSFinDiff = (valueBSUp - valueBSDn) / (2*shift);

		final CharacteristicFunctionModel modelHSUp = new HestonModel(initialValue+shift, riskFreeRate, volatility, discountRate, theta, kappa, xi, rho);
		final CharacteristicFunctionModel modelHSDn = new HestonModel(initialValue-shift, riskFreeRate, volatility, discountRate, theta, kappa, xi, rho);

		final double valueHSUp			= product.getValue(modelHSUp);
		final double valueHSDn			= product.getValue(modelHSDn);
		final double deltaHSFinDiff = (valueHSUp - valueHSDn) / (2*shift);

		final double deltaHSDirect = net.finmath.functions.HestonModel.hestonOptionDelta(initialValue, riskFreeRate, discountRate, volatility, theta, kappa, xi, rho, maturity, strike);

		final long endMillis		= System.currentTimeMillis();

		System.out.println("Delta Black Scholes................:" + deltaBSFinDiff);
		System.out.println("Delta Heston (finite difference)...:" + deltaHSFinDiff);
		System.out.println("Delta Heston (direct)..............:" + deltaHSDirect);

		Assert.assertEquals("Delta (Heston vs Black Scholes)", deltaHSFinDiff, deltaBSFinDiff, 1E-5);
		Assert.assertEquals("Delta (Direct vs Finite Difference)", deltaHSFinDiff, deltaHSDirect, 1E-3);
	}

	@Test
	public void testHeston() throws CalculationException {
		final FourierTransformProduct product = new EuropeanOption(maturity, strike);

		// For xi different from zero we are different from Black Scholes
		final double xi = 0.4639;

		final double shift = 1E-05;
		
		final long startMillis	= System.currentTimeMillis();

		final CharacteristicFunctionModel modelBSUp = new BlackScholesModel(initialValue+shift, riskFreeRate, discountRate, volatility);
		final CharacteristicFunctionModel modelBSDn = new BlackScholesModel(initialValue-shift, riskFreeRate, discountRate, volatility);

		final double valueBSUp			= product.getValue(modelBSUp);
		final double valueBSDn			= product.getValue(modelBSDn);
		final double deltaBSFinDiff = (valueBSUp - valueBSDn) / (2*shift);

		final CharacteristicFunctionModel modelHSUp = new HestonModel(initialValue+shift, riskFreeRate, volatility, discountRate, theta, kappa, xi, rho);
		final CharacteristicFunctionModel modelHSDn = new HestonModel(initialValue-shift, riskFreeRate, volatility, discountRate, theta, kappa, xi, rho);

		final double valueHSUp			= product.getValue(modelHSUp);
		final double valueHSDn			= product.getValue(modelHSDn);
		final double deltaHSFinDiff = (valueHSUp - valueHSDn) / (2*shift);

		final double deltaHSDirect = net.finmath.functions.HestonModel.hestonOptionDelta(initialValue, riskFreeRate, discountRate, volatility, theta, kappa, xi, rho, maturity, strike);

		final long endMillis		= System.currentTimeMillis();

		System.out.println("Delta Black Scholes................:" + deltaBSFinDiff);
		System.out.println("Delta Heston (finite difference)...:" + deltaHSFinDiff);
		System.out.println("Delta Heston (direct)..............:" + deltaHSDirect);

		Assert.assertTrue("Delta (Heston larger tahn Black Scholes)", deltaHSFinDiff > deltaBSFinDiff);
		Assert.assertEquals("Delta (Direct vs Finite Difference)", deltaHSFinDiff, deltaHSDirect, 1E-3);
	}
}
