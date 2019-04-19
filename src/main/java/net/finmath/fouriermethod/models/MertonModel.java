package net.finmath.fouriermethod.models;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Implements the characteristic function of a Merton jump diffusion model.
 *
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class MertonModel implements CharacteristicFunctionModel{

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final double volatility;
	private final double jumpIntensity;
	private final double jumpSizeMean;
	private final double jumpSizeStdDev;

	/**
	 * Construct a Merton jump diffusion model with discount curves for the forward price (i.e. repo rate minus dividend yield) and for discounting.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param jumpIntensity Coefficients of for the jump intensity.
	 * @param jumpSizeMean Jump size mean
	 * @param jumpSizeStdDev Jump size variance.
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 */
	public MertonModel(double initialValue, DiscountCurve discountCurveForForwardRate,
			double volatility,
			double jumpIntensity, double jumpSizeMean, double jumpSizeStdDev,
			DiscountCurve discountCurveForDiscountRate) {
		super();
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.riskFreeRate = Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.discountRate = Double.NaN;
		this.volatility = volatility;
		this.jumpIntensity = jumpIntensity;
		this.jumpSizeMean = jumpSizeMean;
		this.jumpSizeStdDev = jumpSizeStdDev;
	}

	/**
	 * Construct a Merton jump diffusion model with constant rates for the forward price (i.e. repo rate minus dividend yield) and for the discount curve.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate The constant risk free rate for the drift (repo rate of the underlying).
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param jumpIntensity Coefficients of for the jump intensity.
	 * @param jumpSizeMean Jump size mean
	 * @param jumpSizeStdDev Jump size variance.
	 * @param discountRate The constant rate used for discounting.
	 */
	public MertonModel(double initialValue, double riskFreeRate,
			double volatility,
			double jumpIntensity, double jumpSizeMean, double jumpSizeStdDev,
			double discountRate) {
		super();
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		this.discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
		this.jumpIntensity = jumpIntensity;
		this.jumpSizeMean = jumpSizeMean;
		this.jumpSizeStdDev = jumpSizeStdDev;
	}

	/**
	 * Construct a single curve Merton jump diffusion model.
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate The constant risk free rate for the drift (repo rate of the underlying). It is also used for discounting.
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param jumpIntensity Coefficients of for the jump intensity.
	 * @param jumpSizeMean Jump size mean
	 * @param jumpSizeStdDev Jump size variance.
	 */
	public MertonModel(double initialValue, double riskFreeRate, double volatility,
			double jumpIntensity, double jumpSizeMean, double jumpSizeStdDev) {
		this(initialValue,riskFreeRate,volatility,jumpIntensity,jumpSizeMean,jumpSizeStdDev,riskFreeRate);
	}

	@Override
	public CharacteristicFunction apply(double time) {
		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);
		final double transformedMean =  jumpSizeMean - 0.5 * jumpSizeStdDev*jumpSizeStdDev;
		return new CharacteristicFunction() {
			@Override
			public Complex apply(Complex argument) {
				Complex iargument = argument.multiply(Complex.I);

				Complex exponent = (iargument.multiply(transformedMean))
						.add(iargument.multiply(iargument.multiply(jumpSizeStdDev*jumpSizeStdDev/2.0)));

				Complex jumpTransform = ((exponent.exp()).subtract(1.0)).multiply(jumpIntensity*time);

				double jumpTransformCompensator = jumpIntensity*time*(Math.exp(transformedMean+jumpSizeStdDev*jumpSizeStdDev/2.0)-1.0);

				return	iargument
						.multiply(
								iargument
								.multiply(0.5*volatility*volatility*time)
								.add(Math.log(initialValue)-0.5*volatility*volatility*time-logDiscountFactorForForward))
						.add(logDiscountFactorForDiscounting).add(jumpTransform.subtract(jumpTransformCompensator))
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

	public double getInitialValue() {
		return initialValue;
	}

	public DiscountCurve getDiscountCurveForForwardRate() {
		return discountCurveForForwardRate;
	}

	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	public DiscountCurve getDiscountCurveForDiscountRate() {
		return discountCurveForDiscountRate;
	}

	public double getDiscountRate() {
		return discountRate;
	}

	public double getVolatility() {
		return volatility;
	}

	public double getJumpIntensity() {
		return jumpIntensity;
	}

	public double getJumpSizeMean() {
		return jumpSizeMean;
	}

	public double getJumpSizeStdDev() {
		return jumpSizeStdDev;
	}

	@Override
	public String toString() {
		return "MertonModel [initialValue=" + initialValue + ", discountCurveForForwardRate="
				+ discountCurveForForwardRate + ", riskFreeRate=" + riskFreeRate + ", discountCurveForDiscountRate="
				+ discountCurveForDiscountRate + ", discountRate=" + discountRate + ", volatility=" + volatility
				+ ", jumpIntensity=" + jumpIntensity + ", jumpSizeMean=" + jumpSizeMean + ", jumpSizeStdDev="
				+ jumpSizeStdDev + "]";
	}

}
