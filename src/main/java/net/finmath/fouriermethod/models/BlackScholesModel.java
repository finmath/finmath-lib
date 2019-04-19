/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.models;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Implements the characteristic function of a Black Scholes model.
 *
 * @author Christian Fries
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class BlackScholesModel implements CharacteristicFunctionModel {

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final double volatility;



	public BlackScholesModel(double initialValue, DiscountCurve discountCurveForForwardRate,
			double volatility, DiscountCurve discountCurveForDiscountRate) {
		super();
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = Double.NaN;
		this.volatility = volatility;
	}

	public BlackScholesModel(double initialValue, double riskFreeRate, double volatility, double discountRate) {
		super();
		this.initialValue = initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
	}

	public BlackScholesModel(double initialValue, double riskFreeRate, double volatility) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate);
	}

	@Override
	public CharacteristicFunction apply(double time) {
		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);

		return new CharacteristicFunction() {
			@Override
			public Complex apply(Complex argument) {
				Complex iargument = argument.multiply(Complex.I);
				return	iargument
						.multiply(
								iargument
								.multiply(0.5*volatility*volatility*time)
								.add(Math.log(initialValue)-0.5*volatility*volatility*time-logDiscountFactorForForward))
						.add(logDiscountFactorForDiscounting)
						.exp();
			}
		};
	}

	/**
	 * Small helper to calculate rate off the curve or use constant.
	 *
	 * @param time Maturity.
	 * @return The log of the discount factor, i.e., - rate * time.
	 */
	private double getLogDiscountFactorForForward(double time) {
		return discountCurveForForwardRate == null ? -riskFreeRate * time : Math.log(discountCurveForForwardRate.getDiscountFactor(null, time));
	}

	/**
	 * Small helper to calculate rate off the curve or use constant.
	 *
	 * @param time Maturity.
	 * @return The log of the discount factor, i.e., - rate * time.
	 */
	private double getLogDiscountFactorForDiscounting(double time) {
		return discountCurveForDiscountRate == null ? -discountRate * time : Math.log(discountCurveForDiscountRate.getDiscountFactor(null, time));
	}

}
