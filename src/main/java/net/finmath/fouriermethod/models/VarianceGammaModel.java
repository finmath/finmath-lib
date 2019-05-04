package net.finmath.fouriermethod.models;

import java.time.LocalDate;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Implements the characteristic function of a Variance Gamma model.
 * The Variange Gamma model is constructed from a subordinated Brownian motion, where the subordinator is given
 * by a Gamma process.
 * 
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class VarianceGammaModel implements CharacteristicFunctionModel {

	private final LocalDate referenceDate;

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final double sigma;
	private final double theta;
	private final double nu;

	/**
	 * Construct a Variance Gamma model with discount curves for the forward price (i.e. repo rate minus dividend yield) and for discounting.
	 * @param referenceDate
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param \( \sigma \)
	 * @param \( \theta \)
	 * @param \( \nu \)
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 */
	public VarianceGammaModel(LocalDate referenceDate, double initialValue, DiscountCurve discountCurveForForwardRate,
			DiscountCurve discountCurveForDiscountRate, double sigma, double theta, double nu) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = Double.NaN;
		this.sigma = sigma;
		this.theta = theta;
		this.nu = nu;
	}

	/**
	 * Construct a Variance Gamma model with constant rates for the forward price (i.e. repo rate minus dividend yield) and for the discount curve.
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate The constant risk free rate for the drift (repo rate of the underlying).
	 * @param \( \sigma \)
	 * @param \( \theta \)
	 * @param \( \nu \)
	 * @param discountRate The constant rate used for discounting.
	 */
	public VarianceGammaModel(double initialValue, double riskFreeRate, double discountRate, double sigma, double theta,
			double nu) {
		super();
		referenceDate = null;
		this.initialValue = initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
		this.sigma = sigma;
		this.theta = theta;
		this.nu = nu;
	}

	@Override
	public CharacteristicFunction apply(double time) {
		final double logDiscountFactorForForward = this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting = this.getLogDiscountFactorForDiscounting(time);

		return new CharacteristicFunction() {
			@Override
			public Complex apply(Complex argument) {
				Complex iargument = argument.multiply(Complex.I);
				Complex denominator = ((Complex.ONE).subtract(iargument.multiply(theta*nu))).add(argument.multiply(argument).multiply(0.5*sigma*sigma*nu));
				Complex firstLevyExponent = (((Complex.ONE).divide(denominator)).log()).multiply(time/nu);
				Complex compensator =  iargument.multiply(time/nu * Math.log(1/(1.0-theta*nu-0.5*sigma*sigma*nu)));

				return (firstLevyExponent.subtract(compensator)
						.add(iargument.multiply(Math.log(initialValue)-logDiscountFactorForForward))
						.add(logDiscountFactorForDiscounting))
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
	 * @return the sigma
	 */
	public double getSigma() {
		return sigma;
	}

	/**
	 * @return the theta
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * @return the nu
	 */
	public double getNu() {
		return nu;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "VarianceGammaModel [referenceDate=" + referenceDate + ", initialValue=" + initialValue
				+ ", discountCurveForForwardRate=" + discountCurveForForwardRate + ", riskFreeRate=" + riskFreeRate
				+ ", discountCurveForDiscountRate=" + discountCurveForDiscountRate + ", discountRate=" + discountRate
				+ ", sigma=" + sigma + ", theta=" + theta + ", nu=" + nu + "]";
	}
}
