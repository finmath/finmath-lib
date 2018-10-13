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
import net.finmath.modelling.DescribedModel;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.BlackScholesModelDescriptor;
import net.finmath.modelling.productfactory.SingleAssetFourierProductFactory;

/**
 * Implements the characteristic function of a Black Scholes model.
 *
 * @author Christian Fries
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class BlackScholesModel implements ProcessCharacteristicFunctionInterface, DescribedModel<BlackScholesModelDescriptor> {

	private final LocalDate referenceDate;

	private final double initialValue;

	private final DiscountCurveInterface discountCurveForForwardRate;
	private final double riskFreeRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final DiscountCurveInterface discountCurveForDiscountRate;
	private final double discountRate;	// Constant rate, used if discountCurveForForwardRate is null

	private final double volatility;

	/**
	 * Create a model from a model desciptor.
	 *
	 * @param descriptor A Black Scholes model descriptor.
	 */
	public BlackScholesModel(BlackScholesModelDescriptor descriptor) {
		this(
				descriptor.getReferenceDate(),
				descriptor.getInitialValue(),
				descriptor.getDiscountCurveForForwardRate(),
				descriptor.getVolatility(),
				descriptor.getDiscountCurveForDiscountRate()
				);
	}

	public BlackScholesModel(LocalDate referenceDate, double initialValue,
			DiscountCurveInterface discountCurveForForwardRate,
			double volatility, DiscountCurveInterface discountCurveForDiscountRate) {
		super();
		this.referenceDate = referenceDate;
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = discountCurveForForwardRate;
		this.riskFreeRate = Double.NaN;
		this.discountCurveForDiscountRate = discountCurveForDiscountRate;
		this.discountRate = Double.NaN;
		this.volatility = volatility;
	}

	public BlackScholesModel(double initialValue, double riskFreeRate, double volatility, double discountRate) {
		super();
		this.referenceDate = LocalDate.now();
		this.initialValue = initialValue;
		this.discountCurveForForwardRate = null;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		this.discountCurveForDiscountRate = null;
		this.discountRate = discountRate;
	}

	public BlackScholesModel(double initialValue, double riskFreeRate, double volatility) {
		this(initialValue, riskFreeRate, volatility, riskFreeRate);
	}

	@Override
	public CharacteristicFunctionInterface apply(double time) {
		final double logDiscountFactorForForward		= this.getLogDiscountFactorForForward(time);
		final double logDiscountFactorForDiscounting	= this.getLogDiscountFactorForDiscounting(time);

		return argument -> {
			Complex iargument = argument.multiply(Complex.I);
			return	iargument
					.multiply(
							iargument
							.multiply(0.5*volatility*volatility*time)
							.add(Math.log(initialValue)-0.5*volatility*volatility*time-logDiscountFactorForForward))
					.add(logDiscountFactorForDiscounting)
					.exp();
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
	public BlackScholesModelDescriptor getDescriptor() {
		return new BlackScholesModelDescriptor(referenceDate, initialValue, discountCurveForForwardRate, discountCurveForDiscountRate, volatility);
	}

	@Override
	public DescribedProduct<? extends ProductDescriptor> getProductFromDescriptor(ProductDescriptor productDescriptor) {
		return (new SingleAssetFourierProductFactory(referenceDate)).getProductFromDescriptor((SingleAssetProductDescriptor) productDescriptor);
	}
}
