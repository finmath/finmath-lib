/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.models;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunctionInterface;

/**
 * Implements the characteristic function of a Black Scholes model.
 * 
 * @author Christian Fries
 * @author Alessandro Gnoatto
 * @deprecated
 */
public class BlackScholesModel implements ProcessCharacteristicFunctionInterface {

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double volatility;
	private final double discountRate;

	public BlackScholesModel(double initialValue, double riskFreeRate, double volatility, double discountRate) {
		super();
		this.initialValue = initialValue;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		this.discountRate = discountRate;
	}

	public BlackScholesModel(double initialValue, double riskFreeRate, double volatility) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate);
	}

	@Override
	public CharacteristicFunctionInterface apply(double time) {
		return argument -> {
			Complex iargument = argument.multiply(Complex.I);
			return	iargument
					.multiply(
							iargument
							.multiply(0.5*volatility*volatility*time)
							.add(Math.log(initialValue)-0.5*volatility*volatility*time+riskFreeRate*time))
					.exp();
		};
	}
}
