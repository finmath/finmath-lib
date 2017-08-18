/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.models;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunctionInterface;

/**
 * Implements the characteristic function of a Heston model.
 * 
 * The model is
 * \[
 * 	dS(t) = r^{\text{c}} S(t) dt + \sqrt{V(t)} S(t) dW_{1}(t), \quad S(0) = S_{0},
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
 * where \( W \) is a Brownian motion.
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
 * </dl>
 * 
 * @author Christian Fries
 * @author Andy Graf
 * @author Lorenzo Toricelli
 */
public class HestonModel implements ProcessCharacteristicFunctionInterface {

	private final double initialValue;
	private final double riskFreeRate; // Actually the same as the drift (which is not stochastic)
	private final double volatility;

	private final double theta;
	private final double kappa;
	private final double xi;
	private final double rho;

	public HestonModel(double initialValue, double riskFreeRate, double volatility, double theta, double kappa,
			double xi, double rho) {
		super();
		this.initialValue = initialValue;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		this.theta = theta;
		this.kappa = kappa;
		this.xi = xi;
		this.rho = rho;
	}


	@Override
	public CharacteristicFunctionInterface apply(final double time) {

		final double alpha = theta*kappa;
		final double beta = kappa;
		final double sigma = xi;

		return new CharacteristicFunctionInterface() {
			@Override
			public Complex apply(Complex argument) {

				Complex iargument = argument.multiply(Complex.I);

				Complex gamma = iargument.multiply(rho * sigma).subtract(beta).pow(2)
						.subtract(
								iargument.multiply(iargument)
								.add(iargument.multiply(-1)).multiply(0.5)
								.multiply(2 * sigma * sigma))
						.sqrt();

				Complex A = iargument
						.multiply(rho * sigma)
						.subtract(beta)
						.subtract(gamma).multiply((-alpha * time) / (sigma * sigma))
						.subtract(iargument.multiply(rho * sigma).subtract(beta).subtract(gamma)
								.multiply(new Complex(1).divide(gamma.multiply(time).exp()).subtract(1).divide(gamma))
								.multiply(0.5).add(new Complex(1).divide(gamma.multiply(time).exp())).log()
								.add(gamma.multiply(time)).multiply((2 * alpha) / (sigma * sigma)));

				Complex B = iargument.multiply(iargument).add(iargument.multiply(-1)).multiply(0.5)
						.multiply(-2)
						.divide(iargument.multiply(rho * sigma).subtract(beta)
								.add(gamma.multiply(new Complex(1).divide(gamma.multiply(time).exp()).add(1)
										.divide(new Complex(1).divide(gamma.multiply(time).exp()).subtract(1)))));

				return A.add(B.multiply(volatility*volatility)).add(iargument.multiply(Math.log(initialValue) + time * riskFreeRate)).add(-riskFreeRate*time).exp();
			};
		};
	}
}
