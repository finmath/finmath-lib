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
 */
public class BlackScholesModel implements ProcessCharacteristicFunctionInterface {

	private final double initialValue;
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double volatility;

	public BlackScholesModel(double initialValue, double riskFreeRate, double volatility) {
		super();
		this.initialValue = initialValue;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface#apply(double)
	 */
	@Override
	public CharacteristicFunctionInterface apply(double time) {
		return argument -> {
			Complex iargument = argument.multiply(Complex.I);
			return	iargument
					.multiply(
							iargument
							.multiply(0.5*volatility*volatility*time)
							.add(Math.log(initialValue)-0.5*volatility*volatility*time+riskFreeRate*time))
					.add(-riskFreeRate*time)
					.exp();
		};
	}
}
