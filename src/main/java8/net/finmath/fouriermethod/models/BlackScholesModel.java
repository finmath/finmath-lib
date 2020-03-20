/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.models;

import java.time.LocalDate;

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

	private final LocalDate referenceDate;

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final double volatility;


	/**
	 * Create a Black Scholes model (characteristic function)
	 *
	 * @param referenceDate The date representing the time t = 0. All other double times are following {@link net.finmath.time.FloatingpointDate}.
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param volatility \( \sigma \) the volatility level
	 */
	public BlackScholesModel(final LocalDate referenceDate, final double initialValue,
			final DiscountCurve discountCurveForForwardRate, final DiscountCurve discountCurveForDiscountRate, final double volatility) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = Double.NaN;
		this.volatility = volatility;
	}

	/**
	 * Create a Black Scholes model (characteristic function)
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate \( r^{\text{c}} \) - the risk free rate
	 * @param discountRate \( r^{\text{d}} \) - the discount rate
	 * @param volatility \( \sigma \) the volatility level
	 */
	public BlackScholesModel(final double initialValue, final double riskFreeRate, final double discountRate, final double volatility) {
		super();
		referenceDate = null;
		this.initialValue = initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
	}

	/**
	 * Create a Black Scholes model (characteristic function)
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate \( r^{\text{c}} \) - the risk free rate
	 * @param volatility \( \sigma \) the volatility level
	 */
	public BlackScholesModel(final double initialValue, final double riskFreeRate, final double volatility) {
		this(initialValue, riskFreeRate, riskFreeRate, volatility);
	}

	@Override
	public CharacteristicFunction apply(final double time) {
		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);

		return new CharacteristicFunction() {
			@Override
			public Complex apply(final Complex argument) {
				final Complex iargument = argument.multiply(Complex.I);
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
	private double getLogDiscountFactorForForward(final double time) {
		return discountCurveForForwardRate == null ? -riskFreeRate * time : Math.log(discountCurveForForwardRate.getDiscountFactor(null, time));
	}

	/**
	 * Small helper to calculate rate off the curve or use constant.
	 *
	 * @param time Maturity.
	 * @return The log of the discount factor, i.e., - rate * time.
	 */
	private double getLogDiscountFactorForDiscounting(final double time) {
		return discountCurveForDiscountRate == null ? -discountRate * time : Math.log(discountCurveForDiscountRate.getDiscountFactor(null, time));
	}

	/**
	 * @return the referenceDate
	 */
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	/**
	 * @return the initialValue
	 */
	public double getInitialValue() {
		return initialValue;
	}

	/**
	 * @return the discountCurveForForwardRate
	 */
	public DiscountCurve getDiscountCurveForForwardRate() {
		return discountCurveForForwardRate;
	}

	/**
	 * @return the riskFreeRate
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * @return the discountCurveForDiscountRate
	 */
	public DiscountCurve getDiscountCurveForDiscountRate() {
		return discountCurveForDiscountRate;
	}

	/**
	 * @return the discountRate
	 */
	public double getDiscountRate() {
		return discountRate;
	}

	/**
	 * @return the volatility
	 */
	public double getVolatility() {
		return volatility;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BlackScholesModel [initialValue=" + initialValue + ", discountCurveForForwardRate="
				+ discountCurveForForwardRate + ", riskFreeRate=" + riskFreeRate + ", discountCurveForDiscountRate="
				+ discountCurveForDiscountRate + ", discountRate=" + discountRate + ", volatility=" + volatility + "]";
	}

}
