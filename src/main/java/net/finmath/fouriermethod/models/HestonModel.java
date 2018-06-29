/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */

package net.finmath.fouriermethod.models;

import java.time.LocalDate;

import org.apache.commons.math3.complex.Complex;

import net.finmath.fouriermethod.CharacteristicFunctionInterface;
import net.finmath.marketdata.model.curves.DiscountCurveInterface;
import net.finmath.modelling.Model;
import net.finmath.modelling.Product;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.HestonModelDescriptor;
import net.finmath.modelling.descriptor.SingleAssetFourierProductFactory;
import net.finmath.time.FloatingpointDate;

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
 */
public class HestonModel implements Model<HestonModelDescriptor>, ProcessCharacteristicFunctionInterface {

	private final LocalDate referenceDate;

	private final double initialValue;

	private final DiscountCurveInterface discountCurveForForwardRate;
	private final double riskFreeRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final DiscountCurveInterface discountCurveForDiscountRate;
	private final double discountRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final double volatility;

	private final double theta;
	private final double kappa;
	private final double xi;
	private final double rho;

	/**
	 * Create a model from a model desciptor.
	 * 
	 * @param descriptor A Heston model descriptor.
	 */
	public HestonModel(HestonModelDescriptor descriptor) {
		this(
				descriptor.getReferenceDate(),
				descriptor.getInitialValue(),
				descriptor.getDiscountCurveForForwardRate(),
				descriptor.getVolatility(),
				descriptor.getDiscountCurveForDiscountRate(),
				descriptor.getTheta(),
				descriptor.getKappa(),
				descriptor.getXi(),
				descriptor.getRho()
				);
	}

	/**
	 * Create a Heston model (characteristic function)
	 * 
	 * @param referenceDate The date representing the time t = 0. All other double times are following {@link FloatingpointDate}.
	 * @param initialValue \( S_{0} \) - spot - initial value of S
	 * @param discountCurveForForwardRate The curve specifying \( t \mapsto exp(- r^{\text{c}}(t) \cdot t) \) - with \( r^{\text{c}}(t) \) the risk free rate
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param theta \( \theta \) - the mean reversion level of the stochastic volatility
	 * @param kappa \( \kappa \) - the mean reversion speed of the stochastic volatility
	 * @param xi \( \xi \) - the volatility of volatility
	 * @param rho \( \rho \) - the correlation of the Brownian drivers
	 */
	public HestonModel(LocalDate referenceDate, double initialValue, DiscountCurveInterface discountCurveForForwardRate, double volatility, DiscountCurveInterface discountCurveForDiscountRate, double theta, double kappa, double xi, double rho) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.riskFreeRate = Double.NaN; // For safety
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.discountRate = Double.NaN; // For safety
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
	 * @param volatility \( \sigma \) the initial volatility level
	 * @param discountCurveForDiscountRate The curve specifying \( t \mapsto exp(- r^{\text{d}}(t) \cdot t) \) - with \( r^{\text{d}}(t) \) the discount rate
	 * @param theta \( \theta \) - the mean reversion level of the stochastic volatility
	 * @param kappa \( \kappa \) - the mean reversion speed of the stochastic volatility
	 * @param xi \( \xi \) - the volatility of volatility
	 * @param rho \( \rho \) - the correlation of the Brownian drivers
	 */
	public HestonModel(double initialValue, DiscountCurveInterface discountCurveForForwardRate, double volatility, DiscountCurveInterface discountCurveForDiscountRate, double theta, double kappa, double xi, double rho) {
		this(null, initialValue, discountCurveForForwardRate, volatility, discountCurveForDiscountRate, theta, kappa, xi, rho);
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
	public HestonModel(double initialValue, double riskFreeRate, double volatility, double discountRate, double theta, double kappa,
			double xi, double rho) {
		super();
		this.referenceDate = null;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		this.discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
		this.volatility = volatility;
		this.theta = theta;
		this.kappa = kappa;
		this.xi = xi;
		this.rho = rho;
	}

	public HestonModel(double initialValue, double riskFreeRate, double volatility, double theta, double kappa,
			double xi, double rho) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate, theta, kappa, xi, rho);
	}

	@Override
	public CharacteristicFunctionInterface apply(final double time) {

		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);

		return new CharacteristicFunctionInterface() {
			@Override
			public Complex apply(Complex argument) {

				Complex iargument = argument.multiply(Complex.I);

				Complex gamma = iargument.multiply(rho * xi).subtract(kappa).pow(2)
						.subtract(
								iargument.multiply(iargument)
								.add(iargument.multiply(-1)).multiply(0.5)
								.multiply(2 * xi * xi))
						.sqrt();

				Complex a = iargument
						.multiply(rho * xi)
						.subtract(kappa)
						.subtract(gamma).multiply((-theta*kappa * time) / (xi * xi))
						.subtract(iargument.multiply(rho * xi).subtract(kappa).subtract(gamma)
								.multiply(new Complex(1).divide(gamma.multiply(time).exp()).subtract(1).divide(gamma))
								.multiply(0.5).add(new Complex(1).divide(gamma.multiply(time).exp())).log()
								.add(gamma.multiply(time)).multiply((2 * theta*kappa) / (xi * xi)));

				Complex b = iargument.multiply(iargument).add(iargument.multiply(-1)).multiply(-1)
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

	@Override
	public HestonModelDescriptor getDescriptor() {
		return new HestonModelDescriptor(referenceDate, initialValue, discountCurveForForwardRate, discountCurveForDiscountRate, volatility, theta, kappa, xi, rho);
	}

	@Override
	public Product<? extends ProductDescriptor> getProductFromDesciptor(ProductDescriptor productDescriptor) {
		return (new SingleAssetFourierProductFactory()).getProductFromDescription((SingleAssetProductDescriptor) productDescriptor);
	}
}
