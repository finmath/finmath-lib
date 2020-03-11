/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */
package net.finmath.fouriermethod.models;

import java.time.LocalDate;
import java.util.Arrays;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunction;
import net.finmath.marketdata.model.curves.DiscountCurve;

/**
 * Implements the characteristic function of a Bates model.
 *
 * The Bates model for an underlying \( S \) is given by
 * \[
 * 	dS(t) = r^{\text{c}} S(t) dt + \sqrt{V(t)} S(t) dW_{1}(t) + S dJ, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dV(t) = \kappa ( \theta - V(t) ) dt + \xi \sqrt{V(t)} dW_{2}(t), \quad V(0) = \sigma^2,
 * \]
 * \[
 * 	dW_{1} dW_{2} = \rho dt
 * \]
 * \[
 * 	dN(t) = r^{\text{d}} N(t) dt, \quad N(0) = N_{0},
 * \]
 * where \( W \) is Brownian motion and \( J \)  is a jump process (compound Poisson process).
 *
 * The free parameters of this model are:
 * <dl>
 * 	<dt>\( S_{0} \)</dt> <dd>spot - initial value of S</dd>
 * 	<dt>\( r \)</dt> <dd>the risk free rate</dd>
 * 	<dt>\( \sigma \)</dt> <dd>the initial volatility level</dd>
 * 	<dt>\( \xi \)</dt> <dd>the volatility of volatility</dd>
 * 	<dt>\( \theta \)</dt> <dd>the mean reversion level of the stochastic volatility</dd>
 * 	<dt>\( \kappa \)</dt> <dd>the mean reversion speed of the stochastic volatility</dd>
 * 	<dt>\( \rho \)</dt> <dd>the correlation of the Brownian drivers</dd>
 * 	<dt>\( a \)</dt> <dd>the jump size mean</dd>
 * 	<dt>\( b \)</dt> <dd>the jump size standard deviation</dd>
 * </dl>
 *
 * The process \( J \) is given by \( J(t) = \sum_{i=1}^{N(t)} (Y_{i}-1) \), where
 * \( \log(Y_{i}) \) are i.i.d. normals with mean \( a - \frac{1}{2} b^{2} \) and standard deviation \( b \).
 * Here \( a \) is the jump size mean and \( b \) is the jump size std. dev.
 *
 *  The model can be rewritten as \( S = \exp(X) \), where
 * \[
 * 	dX = \mu dt + \sqrt{V(t)} dW + dJ^{X}, \quad X(0) = \log(S_{0}),
 * \]
 * with
 * \[
 * 	J^{X}(t) = \sum_{i=1}^{N(t)} \log(Y_{i})
 * \]
 * with \( \mu = r - \frac{1}{2} \sigma^2 - (exp(a)-1) \lambda \).
 *
 *
 * @author Christian Fries
 * @author Andy Graf
 * @author Lorenzo Toricelli
 * @version 1.0
 */
public class BatesModel implements CharacteristicFunctionModel {

	private final LocalDate referenceDate;

	private final double initialValue;

	private final DiscountCurve discountCurveForForwardRate;
	private final double riskFreeRate; // Actually the same as the drift (which is not stochastic)

	private final DiscountCurve discountCurveForDiscountRate;
	private final double discountRate;

	private final double[] volatility;

	private final double[] alpha;
	private final double[] beta;
	private final double[] sigma;
	private final double[] rho;

	private final double[] lambda; //3 constants
	private final double k;
	private final double delta;

	private final int numberOfFactors;

	/**
	 * Create a two factor Bates model.
	 *
	 * @param referenceDate The date representing the time t = 0. All other double times are following {@link net.finmath.time.FloatingpointDate}.
	 * @param initialValue Initial value of S.
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param volatility Square root of initial value of the stochastic variance process V.
	 * @param alpha The parameter alpha/beta is the mean reversion level of the variance process V.
	 * @param beta Mean reversion speed of variance process V.
	 * @param sigma Volatility of volatility.
	 * @param rho Correlations of the Brownian drives (underlying, variance).
	 * @param lambda Coefficients of for the jump intensity.
	 * @param k Jump size mean.
	 * @param delta Jump size variance.
	 */
	public BatesModel(
			final LocalDate referenceDate,
			final double initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final DiscountCurve discountCurveForDiscountRate,
			final double[] volatility,
			final double[] alpha,
			final double[] beta,
			final double[] sigma,
			final double[] rho,
			final double[] lambda,
			final double k, final double delta
			) {
		super();
		this.referenceDate =  referenceDate;
		this.initialValue	= initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		riskFreeRate	= Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		discountRate	= Double.NaN;
		this.volatility		= volatility;
		this.alpha			= alpha;
		this.beta			= beta;
		this.sigma			= sigma;
		this.rho			= rho;
		this.lambda			= lambda;
		this.k				= k;
		this.delta			= delta;

		numberOfFactors = alpha.length;
	}

	/**
	 * Create a two factor Bates model.
	 *
	 * @param initialValue Initial value of S.
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param volatility Square root of initial value of the stochastic variance process V.
	 * @param alpha The parameter alpha/beta is the mean reversion level of the variance process V.
	 * @param beta Mean reversion speed of variance process V.
	 * @param sigma Volatility of volatility.
	 * @param rho Correlations of the Brownian drives (underlying, variance).
	 * @param lambda Coefficients of for the jump intensity.
	 * @param k Jump size mean.
	 * @param delta Jump size variance.
	 */
	public BatesModel(
			final double initialValue,
			final DiscountCurve discountCurveForForwardRate,
			final DiscountCurve discountCurveForDiscountRate,
			final double[] volatility,
			final double[] alpha,
			final double[] beta,
			final double[] sigma,
			final double[] rho,
			final double[] lambda,
			final double k, final double delta
			) {
		this(null, initialValue, discountCurveForForwardRate, discountCurveForDiscountRate,
				volatility,alpha,beta,sigma,rho,lambda,k,delta);
	}

	/**
	 * Create a two factor Bates model.
	 *
	 * @param initialValue Initial value of S.
	 * @param riskFreeRate Risk free rate.
	 * @param discountRate The rate used for discounting.
	 * @param volatility Square root of initial value of the stochastic variance process V.
	 * @param alpha The parameter alpha/beta is the mean reversion level of the variance process V.
	 * @param beta Mean reversion speed of variance process V.
	 * @param sigma Volatility of volatility.
	 * @param rho Correlations of the Brownian drives (underlying, variance).
	 * @param lambda Coefficients of for the jump intensity.
	 * @param k Jump size mean.
	 * @param delta Jump size variance.
	 */
	public BatesModel(
			final double initialValue,
			final double riskFreeRate,
			final double discountRate,
			final double[] volatility,
			final double[] alpha,
			final double[] beta,
			final double[] sigma,
			final double[] rho,
			final double[] lambda,
			final double k,
			final double delta
			) {
		referenceDate = null;
		this.initialValue = initialValue;
		discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
		this.volatility		= volatility;
		this.alpha			= alpha;
		this.beta			= beta;
		this.sigma			= sigma;
		this.rho			= rho;
		this.lambda			= lambda;
		this.k				= k;
		this.delta			= delta;

		numberOfFactors = alpha.length;
	}

	/**
	 * Create a one factor Bates model.
	 *
	 * @param initialValue Initial value of S.
	 * @param riskFreeRate Risk free rate.
	 * @param volatility Square root of initial value of the stochastic variance process V.
	 * @param alpha The parameter alpha/beta is the mean reversion level of the variance process V.
	 * @param beta Mean reversion speed of variance process V.
	 * @param sigma Volatility of volatility.
	 * @param rho Correlations of the Brownian drives (underlying, variance).
	 * @param lambdaZero Constant part of the jump intensity.
	 * @param lambdaOne Coefficients of the jump intensity, linear in variance.
	 * @param k Jump size mean.
	 * @param delta Jump size variance.
	 */
	public BatesModel(
			final double initialValue,
			final double riskFreeRate,
			final double volatility,
			final double alpha,
			final double beta,
			final double sigma,
			final double rho,
			final double lambdaZero,
			final double lambdaOne,
			final double k,
			final double delta
			) {
		this(initialValue, riskFreeRate,riskFreeRate,
				new double[]{ volatility },
				new double[]{ alpha },
				new double[]{ beta },
				new double[]{ sigma },
				new double[]{ rho },
				new double[]{ lambdaZero, lambdaOne },
				k,
				delta
				);
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface#apply(double)
	 */
	@Override
	public CharacteristicFunction apply(final double time) {

		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);

		return new CharacteristicFunction() {
			@Override
			public Complex apply(final Complex argument) {

				final Complex iargument = argument.multiply(Complex.I);

				final Complex c = iargument
						.multiply(iargument)
						.add(iargument.multiply(-1))
						.multiply(0.5*delta*delta)
						.exp()
						.multiply(new Complex(1+k).pow(iargument))
						.add(-1)
						.add(iargument.multiply(-k));

				final Complex[] gamma = new Complex[numberOfFactors];
				final Complex[] a = new Complex[numberOfFactors];
				final Complex[] b = new Complex[numberOfFactors];
				for(int i = 0; i < numberOfFactors; i++) {
					gamma[i] = iargument
							.multiply(rho[i]*sigma[i])
							.subtract(beta[i])
							.pow(2)
							.subtract(
									iargument.multiply(iargument)
									.add(iargument.multiply(-1))
									.multiply(0.5)
									.add(c.multiply(lambda[i+1]))
									.multiply(2*sigma[i]*sigma[i])
									)
							.sqrt()
							;


					a[i] =
							iargument
							.multiply(rho[i] * sigma[i])
							.subtract(beta[i])
							.subtract(gamma[i])
							.multiply((-alpha[i]*time)/(sigma[i]*sigma[i]))
							.subtract(iargument
									.multiply(rho[i]*sigma[i])
									.subtract(beta[i])
									.subtract(gamma[i])
									.multiply(new Complex(1).divide(gamma[i].multiply(time).exp())
											.subtract(1)
											.divide(gamma[i])
											)
									.multiply(0.5)
									.add(new Complex(1).divide(gamma[i].multiply(time).exp()))
									.log()
									.add(gamma[i].multiply(time))
									.multiply((2*alpha[i])/(sigma[i]*sigma[i]))
									)
							;

					b[i] = iargument
							.multiply(iargument)
							.add(iargument.multiply(-1))
							.multiply(0.5)
							.add(c.multiply(lambda[i+1]))
							.multiply(-2)
							.divide(iargument
									.multiply(rho[i] * sigma[i])
									.subtract(beta[i])
									.add(gamma[i]
											.multiply(new Complex(1).divide(gamma[i].multiply(time).exp())
													.add(1)
													.divide(new Complex(1).divide(gamma[i].multiply(time).exp())
															.subtract(1)
															)
													)
											)
									)
							;

				}

				Complex characteristicFunction =
						a[0]
								.add(b[0].multiply(volatility[0]))
								.add(c.multiply(time*lambda[0]))
								.add(iargument.multiply(Math.log(initialValue) - logDiscountFactorForForward))
								.add(logDiscountFactorForDiscounting);

				if(numberOfFactors == 2) {
					characteristicFunction = characteristicFunction
							.add(a[1])
							.add(b[1].multiply(volatility[1]));
				}

				characteristicFunction = characteristicFunction.exp();

				return characteristicFunction;
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
	 * @return the riskFreeRate
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * @return the volatility
	 */
	public double[] getVolatility() {
		return volatility;
	}

	/**
	 * @return the discountRate
	 */
	public double getDiscountRate() {
		return discountRate;
	}

	/**
	 * @return the alpha
	 */
	public double[] getAlpha() {
		return alpha;
	}

	/**
	 * @return the beta
	 */
	public double[] getBeta() {
		return beta;
	}

	/**
	 * @return the sigma
	 */
	public double[] getSigma() {
		return sigma;
	}

	/**
	 * @return the rho
	 */
	public double[] getRho() {
		return rho;
	}

	/**
	 * @return the lambda
	 */
	public double[] getLambda() {
		return lambda;
	}

	/**
	 * @return the k
	 */
	public double getK() {
		return k;
	}

	/**
	 * @return the delta
	 */
	public double getDelta() {
		return delta;
	}

	/**
	 * @return the numberOfFactors
	 */
	public int getNumberOfFactors() {
		return numberOfFactors;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "BatesModel [initialValue=" + initialValue + ", riskFreeRate=" + riskFreeRate + ", volatility="
				+ Arrays.toString(volatility) + ", discountRate=" + discountRate + ", alpha=" + Arrays.toString(alpha)
				+ ", beta=" + Arrays.toString(beta) + ", sigma=" + Arrays.toString(sigma) + ", rho="
				+ Arrays.toString(rho) + ", lambda=" + Arrays.toString(lambda) + ", k=" + k + ", delta=" + delta
				+ ", numberOfFactors=" + numberOfFactors + "]";
	}
}
