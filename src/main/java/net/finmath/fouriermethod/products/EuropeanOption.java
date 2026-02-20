/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */
package net.finmath.fouriermethod.products;

import org.apache.commons.math3.complex.Complex;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.modelling.products.CallOrPut;

/**
 * Implements valuation of a European option on a single asset.
 *
 * Given a model for an asset <i>S</i>, the European option with strike <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>max((S(T) - K) * CallOrPut , 0)</i> in <i>T</i>
 * <br>
 *
 * The class implements the characteristic function of the call option
 * payoff, i.e., its Fourier transform.
 *
 * @author Christian Fries
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class EuropeanOption extends AbstractFourierTransformProduct {

	private final String underlyingName;
	private final double maturity;
	private final double strike;
	private final CallOrPut callOrPutSign;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index <code>underlyingIndex</code> from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff max(sign * (S(T)-K),0).
	 * @param strike The strike K in the option payoff max(sign * (S(T)-K),0).
	 * @param callOrPutSign The sign in the payoff.
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike, final double callOrPutSign) {
		super();
		this.underlyingName	= underlyingName;
		this.maturity		= maturity;
		this.strike			= strike;
		if(callOrPutSign == 1.0) {
			this.callOrPutSign = CallOrPut.CALL;
		}else if(callOrPutSign == - 1.0) {
			this.callOrPutSign = CallOrPut.PUT;
		}else {
			throw new IllegalArgumentException("Unknown option type");
		}
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index <code>underlyingIndex</code> from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff max(sign * (S(T)-K),0).
	 * @param strike The strike K in the option payoff max(sign * (S(T)-K),0).
	 * @param callOrPutSign The sign in the payoff.
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike, final CallOrPut callOrPutSign) {
		super();
		this.underlyingName	= underlyingName;
		this.maturity		= maturity;
		this.strike			= strike;
		this.callOrPutSign	= callOrPutSign;
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param callOrPutSign The sign in the payoff.
	 * @param underlyingIndex The index of the underlying to be fetched from the model.
	 */
	public EuropeanOption(final double maturity, final double strike, final double callOrPutSign) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		if(callOrPutSign == 1.0) {
			this.callOrPutSign = CallOrPut.CALL;
		}else if(callOrPutSign == - 1.0) {
			this.callOrPutSign = CallOrPut.PUT;
		}else {
			throw new IllegalArgumentException("Unknown option type");
		}
		this.underlyingName	= null;		// Use underlyingIndex
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param callOrPutSign The sign in the payoff.
	 * @param underlyingIndex The index of the underlying to be fetched from the model.
	 */
	public EuropeanOption(final double maturity, final double strike, final CallOrPut callOrPutSign) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		this.callOrPutSign		= callOrPutSign;
		this.underlyingName	= null;		// Use underlyingIndex
	}
	
	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index <code>underlyingIndex</code> from the model - single asset case).
	 * @param underlyingName Name of the underlying
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public EuropeanOption(final String underlyingName, final double maturity, final double strike) {
		this(underlyingName, maturity, strike, 1.0);
	}


	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public EuropeanOption(final double maturity, final double strike) {
		this(maturity, strike, 1.0);
	}


	@Override
	public Complex apply(final Complex argument) {
		final Complex iargument = argument.multiply(Complex.I);
		final Complex exponent = (iargument).add(1);
		final Complex numerator = (new Complex(strike)).pow(exponent);
		final Complex denominator = (argument.multiply(argument)).subtract(iargument);

		return numerator.divide(denominator).negate();
	}

	@Override
	public double getValue(final CharacteristicFunctionModel model) throws CalculationException {
		if(callOrPutSign == CallOrPut.CALL) {
			//It is a call, just use the existing implementation
			return super.getValue(model);
		}else {
			double df = model.getDiscountCurveForDiscountRate() == null ? 
					Math.exp(- model.getDiscountRate()) 
					: model.getDiscountCurveForDiscountRate().getDiscountFactor(maturity);
			//It is a put, use the put call parity
			return super.getValue(model) - model.getInitialValue() + this.strike * df;
		}
		
	}

	public String getUnderlyingName() {
		return underlyingName;
	}

	@Override
	public double getMaturity() {
		return maturity;
	}

	public double getStrike() {
		return strike;
	}

	@Override
	public double getIntegrationDomainImagLowerBound() {
		return 0.5;
	}

	@Override
	public double getIntegrationDomainImagUpperBound() {
		return 2.5;
	}

}
