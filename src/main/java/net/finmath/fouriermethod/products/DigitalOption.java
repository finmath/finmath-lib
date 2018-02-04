/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */
package net.finmath.fouriermethod.products;

import org.apache.commons.math3.complex.Complex;

import net.finmath.analytic.model.curves.DiscountCurve;

/**
 * Implements valuation of a European option on a single asset.
 * 
 * Given a model for an asset <i>S</i>, the European option with strike <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>indicator(S(T) - K)</i> in <i>T</i>
 * <br>
 * 
 * The class implements the characteristic function of the call option
 * payoff, i.e., its Fourier transform.
 * 
 * @author Christian Fries
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class DigitalOption extends AbstractProductFourierTransform {

	private final double maturity;
	private final double strike;
	private final String nameOfUnderliyng;
	private final DiscountCurve discountCurve;
	
	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public DigitalOption(double maturity, double strike, DiscountCurve discountCurve) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		this.nameOfUnderliyng	= null;		// Use asset with index 0
		this.discountCurve      = discountCurve;
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.CharacteristicFunctionInterface#apply(org.apache.commons.math3.complex.Complex)
	 */
	@Override
	public Complex apply(Complex argument) {
		Complex iargument = argument.multiply(Complex.I);
		Complex exponent = iargument.add(1.0);
		Complex numerator = (new Complex(strike)).pow(exponent.subtract(1.0)).multiply(exponent);
		Complex denominator = (argument.multiply(argument)).subtract(iargument);	

		return numerator.divide(denominator);
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.AbstractProductFourierTransform#getMaturity()
	 */
	@Override
	public double getMaturity() {
		return maturity;
	}
	
	public DiscountCurve getDiscountCurve() {
		return this.discountCurve;
	}
	
	public String getNameOfUnderlying() {
		return this.nameOfUnderliyng;
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.AbstractProductFourierTransform#getDomainImagLowerBound()
	 */
	@Override
	public double getIntegrationDomainImagLowerBound() {
		return 0.5;
	}

	/* (non-Javadoc)
	 * @see net.finmath.fouriermethod.products.AbstractProductFourierTransform#getDomainImagUpperBound()
	 */
	@Override
	public double getIntegrationDomainImagUpperBound() {
		return 2.5;
	}
}
