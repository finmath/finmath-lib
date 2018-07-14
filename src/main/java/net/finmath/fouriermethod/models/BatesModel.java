/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */
package net.finmath.fouriermethod.models;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunctionInterface;

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
 */
public class BatesModel implements ProcessCharacteristicFunctionInterface {

	private final double initialValue;
	private final double riskFreeRate; // Actually the same as the drift (which is not stochastic)
	private final double[] volatility;
	private final double discountRate;

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
	 * @param initialValue Initial value of S.
	 * @param riskFreeRate Risk free rate.
	 * @param volatility Square root of initial value of the stochastic variance process V.
	 * @param discountRate Rate used for the discount factor.
	 * @param alpha The parameter alpha/beta is the mean reversion level of the variance process V.
	 * @param beta Mean reversion speed of variance process V.
	 * @param sigma Volatility of volatility.
	 * @param rho Correlations of the Brownian drives (underlying, variance).
	 * @param lambda Coefficients of for the jump intensity.
	 * @param k Jump size mean.
	 * @param delta Jump size variance.
	 */
	public BatesModel(
			double initialValue,
			double riskFreeRate,
			double[] volatility,
			double discountRate,
			double[] alpha,
			double[] beta,
			double[] sigma,
			double[] rho,
			double[] lambda,
			double k,
			double delta
			) {
		super();
		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;
		this.discountRate	= discountRate;
		this.alpha			= alpha;
		this.beta			= beta;
		this.sigma			= sigma;
		this.rho			= rho;
		this.lambda			= lambda;
		this.k				= k;
		this.delta			= delta;

		this.numberOfFactors = alpha.length;
	}

	/**
	 * Create a two factor Bates model.
	 *
	 * @param initialValue Initial value of S.
	 * @param riskFreeRate Risk free rate.
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
			double initialValue,
			double riskFreeRate,
			double[] volatility,
			double[] alpha,
			double[] beta,
			double[] sigma,
			double[] rho,
			double[] lambda,
			double k,
			double delta
			) {
		this(
				initialValue,
				riskFreeRate,
				volatility,
				riskFreeRate,
				alpha,
				beta,
				sigma,
				rho,
				lambda,
				k,
				delta
				);
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
			double initialValue,
			double riskFreeRate,
			double volatility,
			double alpha,
			double beta,
			double sigma,
			double rho,
			double lambdaZero,
			double lambdaOne,
			double k,
			double delta
			) {
		this(initialValue, riskFreeRate,
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
	public CharacteristicFunctionInterface apply(final double time) {
		return new CharacteristicFunctionInterface() {
			@Override
			public Complex apply(Complex argument) {

				Complex iargument = argument.multiply(Complex.I);

				Complex c = iargument
						.multiply(iargument)
						.add(iargument.multiply(-1))
						.multiply(0.5*delta*delta)
						.exp()
						.multiply(new Complex(1+k).pow(iargument))
						.add(-1)
						.add(iargument.multiply(-k));

				Complex[] gamma = new Complex[numberOfFactors];
				Complex[] a = new Complex[numberOfFactors];
				Complex[] b = new Complex[numberOfFactors];
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
								.add(iargument.multiply(Math.log(initialValue)+time*riskFreeRate))
								.add(-discountRate*time);

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
}

