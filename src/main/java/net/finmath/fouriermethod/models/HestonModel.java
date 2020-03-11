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
 * Implements the characteristic function of a Heston model.
 *
 * The model is
 * \[
 * 	dS(t) = r^{\text{c}}(t) S(t) dt + \sqrt{V(t)} S(t) dW_{1}(t), \quad S(0) = S_{0},
 * \]
 * \[
 * 	dV(t) = \kappa ( \theta - V(t) ) dt + \xi \sqrt{V(t)} dW_{2}(t), \quad V(0) = \sigma^2,
 * \]
 * \[
 * 	dW_{1} dW_{2} = \rho dt
 * \]
 * \[
 * 	dN(t) = r^{\text{d}}(t) N(t) dt, \quad N(0) = N_{0},
 * \]
 * where \( W \) is a Brownian motion.
 *
 * The model allows to specify two independent rate for forwarding (\( r^{\text{c}} \)) and discounting (\( r^{\text{d}} \)).
 * It thus allow for a simple modelling of a funding / collateral curve (via (\( r^{\text{d}} \)) and/or the specification of
 * a dividend yield.
 *
 * The free parameters of this model are:
 * <dl>
 * 	<dt>\( S_{0} \)</dt> <dd>spot - initial value of S</dd>
 * 	<dt>\( r^{\text{c}} \)</dt> <dd>the risk free rate (may be provided as a curve or a constant)</dd>
 * 	<dt>\( \sigma \)</dt> <dd>the initial volatility level</dd>
 * 	<dt>\( r^{\text{d}} \)</dt> <dd>the discount rate (may be provided as a curve or a constant)</dd>
 * 	<dt>\( \xi \)</dt> <dd>the volatility of volatility</dd>
 * 	<dt>\( \theta \)</dt> <dd>the mean reversion level of the stochastic volatility</dd>
 * 	<dt>\( \kappa \)</dt> <dd>the mean reversion speed of the stochastic volatility</dd>
 * 	<dt>\( \rho \)</dt> <dd>the correlation of the Brownian drivers</dd>
 * </dl>
 *
 * @author Christian Fries
 * @author Andy Graf
 * @author Lorenzo Toricelli
 * @version 1.0
 */
public class HestonModel implements CharacteristicFunctionModel {

	private final LocalDate referenceDate;

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final double volatility;

	private final double theta;
	private final double kappa;
	private final double xi;
	private final double rho;

	/**
	 * Create a Heston model (characteristic function)
	 *
	 * @param referenceDate The date representing the time t = 0. All other double times are following {@link net.finmath.time.FloatingpointDate}.
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param theta \( \theta \) - the mean reversion level of the stochastic volatility
	 * @param kappa \( \kappa \) - the mean reversion speed of the stochastic volatility
	 * @param xi \( \xi \) - the volatility of volatility
	 * @param rho \( \rho \) - the correlation of the Brownian drivers
	 */
	public HestonModel(final LocalDate referenceDate, final double initialValue, final DiscountCurve discountCurveForForwardRate, final DiscountCurve discountCurveForDiscountRate, final double volatility, final double theta, final double kappa, final double xi, final double rho) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate = Double.NaN; // For safety
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate = Double.NaN; // For safety
		this.volatility = volatility;
		this.theta = theta;
		this.kappa = kappa;
		this.xi = xi;
		this.rho = rho;
	}

	/**
	 * Create a Heston model (characteristic function)
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param theta \( \theta \) - the mean reversion level of the stochastic volatility
	 * @param kappa \( \kappa \) - the mean reversion speed of the stochastic volatility
	 * @param xi \( \xi \) - the volatility of volatility
	 * @param rho \( \rho \) - the correlation of the Brownian drivers
	 */
	public HestonModel(final double initialValue, final DiscountCurve discountCurveForForwardRate, final DiscountCurve discountCurveForDiscountRate, final double volatility, final double theta, final double kappa, final double xi, final double rho) {
		this(null, initialValue, discountCurveForForwardRate, discountCurveForDiscountRate, volatility, theta, kappa, xi, rho);
	}

	/**
	 * Create a Heston model (characteristic function)
	 *
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param riskFreeRate \( r^{\text{c}} \) - the risk free rate
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param discountRate \( r^{\text{d}} \) - the discount rate
	 * @param theta \( \theta \) - the mean reversion level of the stochastic volatility
	 * @param kappa \( \kappa \) - the mean reversion speed of the stochastic volatility
	 * @param xi \( \xi \) - the volatility of volatility
	 * @param rho \( \rho \) - the correlation of the Brownian drivers
	 */
	public HestonModel(final double initialValue, final double riskFreeRate, final double volatility, final double discountRate, final double theta, final double kappa,
			final double xi, final double rho) {
		super();
		referenceDate = null;
		this.initialValue = initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
		this.volatility = volatility;
		this.theta = theta;
		this.kappa = kappa;
		this.xi = xi;
		this.rho = rho;
	}

	public HestonModel(final double initialValue, final double riskFreeRate, final double volatility, final double theta, final double kappa,
			final double xi, final double rho) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate, theta, kappa, xi, rho);
	}

	@Override
	public CharacteristicFunction apply(final double time) {

		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);

		return new CharacteristicFunction() {
			@Override
			public Complex apply(final Complex argument) {

				final Complex iargument = argument.multiply(Complex.I);

				final Complex gamma = iargument.multiply(rho * xi).subtract(kappa).pow(2)
						.subtract(
								iargument.multiply(iargument)
								.add(iargument.multiply(-1)).multiply(0.5)
								.multiply(2 * xi * xi))
						.sqrt();

				final Complex a = iargument
						.multiply(rho * xi)
						.subtract(kappa)
						.subtract(gamma).multiply((-theta*kappa * time) / (xi * xi))
						.subtract(iargument.multiply(rho * xi).subtract(kappa).subtract(gamma)
								.multiply(new Complex(1).divide(gamma.multiply(time).exp()).subtract(1).divide(gamma))
								.multiply(0.5).add(new Complex(1).divide(gamma.multiply(time).exp())).log()
								.add(gamma.multiply(time)).multiply((2 * theta*kappa) / (xi * xi)));

				final Complex b = iargument.multiply(iargument).add(iargument.multiply(-1)).multiply(-1)
						.divide(iargument.multiply(rho * xi).subtract(kappa)
								.add(gamma.multiply(new Complex(1).divide(gamma.multiply(time).exp()).add(1)
										.divide(new Complex(1).divide(gamma.multiply(time).exp()).subtract(1)))));

				return a.add(b.multiply(volatility*volatility)).add(iargument.multiply(Math.log(initialValue) - logDiscountFactorForForward)).add(logDiscountFactorForDiscounting).exp();
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
	 * @return the theta
	 */
	public double getTheta() {
		return theta;
	}

	/**
	 * @return the kappa
	 */
	public double getKappa() {
		return kappa;
	}

	/**
	 * @return the xi
	 */
	public double getXi() {
		return xi;
	}

	/**
	 * @return the rho
	 */
	public double getRho() {
		return rho;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "HestonModel [referenceDate=" + referenceDate + ", initialValue=" + initialValue
				+ ", discountCurveForForwardRate=" + discountCurveForForwardRate + ", riskFreeRate=" + riskFreeRate
				+ ", discountCurveForDiscountRate=" + discountCurveForDiscountRate + ", discountRate=" + discountRate
				+ ", volatility=" + volatility + ", theta=" + theta + ", kappa=" + kappa + ", xi=" + xi + ", rho=" + rho
				+ "]";
	}

}
