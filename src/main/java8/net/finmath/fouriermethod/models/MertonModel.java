package net.finmath.fouriermethod.models;

import java.time.LocalDate;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Implements the characteristic function of a Merton jump diffusion model.
 *
 * The model is
 * \[
 * 	dS = \mu S dt + \sigma S dW + S dJ, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * where \( W \) is Brownian motion and \( J \)  is a jump process (compound Poisson process).
 *
 * The process \( J \) is given by \( J(t) = \sum_{i=1}^{N(t)} (Y_{i}-1) \), where
 * \( \log(Y_{i}) \) are i.i.d. normals with mean \( a - \frac{1}{2} b^{2} \) and standard deviation \( b \).
 * Here \( a \) is the jump size mean and \( b \) is the jump size std. dev.
 *
 *
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class MertonModel implements CharacteristicFunctionModel{

	private final LocalDate referenceDate;

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
	 * @param referenceDate The date representing the time t = 0. All other double times are following {@link net.finmath.time.FloatingpointDate}.
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param jumpIntensity Coefficients of for the jump intensity.
	 * @param jumpSizeMean Jump size mean
	 * @param jumpSizeStdDev Jump size variance.
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 */
	public MertonModel(final LocalDate referenceDate, final double initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final DiscountCurve discountCurveForDiscountRate, final double volatility, final double jumpIntensity,
			final double jumpSizeMean, final double jumpSizeStdDev) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = Double.NaN;
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
	public MertonModel(final double initialValue, final double riskFreeRate,
			final double discountRate,
			final double volatility, final double jumpIntensity, final double jumpSizeMean,
			final double jumpSizeStdDev) {
		super();
		referenceDate = null;
		this.initialValue = initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		discountCurveForDiscountRate = null;
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
	public MertonModel(final double initialValue, final double riskFreeRate, final double volatility,
			final double jumpIntensity, final double jumpSizeMean, final double jumpSizeStdDev) {
		this(initialValue,riskFreeRate,riskFreeRate,volatility,jumpIntensity,jumpSizeMean,jumpSizeStdDev);
	}

	@Override
	public CharacteristicFunction apply(final double time) {
		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);
		final double transformedMean =  jumpSizeMean - 0.5 * jumpSizeStdDev*jumpSizeStdDev;
		return new CharacteristicFunction() {
			@Override
			public Complex apply(final Complex argument) {
				final Complex iargument = argument.multiply(Complex.I);

				final Complex exponent = (iargument.multiply(transformedMean))
						.add(iargument.multiply(iargument.multiply(jumpSizeStdDev*jumpSizeStdDev/2.0)));

				final Complex jumpTransform = ((exponent.exp()).subtract(1.0)).multiply(jumpIntensity*time);

				final double jumpTransformCompensator = jumpIntensity*time*(Math.exp(transformedMean+jumpSizeStdDev*jumpSizeStdDev/2.0)-1.0);

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

	/**
	 * @return the jumpIntensity
	 */
	public double getJumpIntensity() {
		return jumpIntensity;
	}

	/**
	 * @return the jumpSizeMean
	 */
	public double getJumpSizeMean() {
		return jumpSizeMean;
	}

	/**
	 * @return the jumpSizeStdDev
	 */
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
