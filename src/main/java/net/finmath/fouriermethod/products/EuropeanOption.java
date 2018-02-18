/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 23.03.2014
 */
package net.finmath.fouriermethod.products;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.math3.complex.Complex;

import net.finmath.exception.CalculationException;
import net.finmath.experimental.model.implementation.SingleAssetEuropeanOptionProductDescriptor;
import net.finmath.fouriermethod.models.ProcessCharacteristicFunctionInterface;
import net.finmath.modelling.Model;
import net.finmath.modelling.Product;

/**
 * Implements valuation of a European option on a single asset.
 * 
 * Given a model for an asset <i>S</i>, the European option with strike <i>K</i>, maturity <i>T</i>
 * pays
 * <br>
 * 	<i>max(S(T) - K , 0)</i> in <i>T</i>
 * <br>
 * 
 * The class implements the characteristic function of the call option
 * payoff, i.e., its Fourier transform.
 * 
 * @author Christian Fries
 * @author Alessandro Gnoatto
 * @version 1.0
 */
public class EuropeanOption extends AbstractProductFourierTransform implements Product<SingleAssetEuropeanOptionProductDescriptor> {

	private final String underlyingName;
	private final double maturity;
	private final double strike;
	
	/**
	 * Create the product from a descriptor.
	 * 
	 * @param descriptor A descriptor of the product.
	 */
	public EuropeanOption(SingleAssetEuropeanOptionProductDescriptor descriptor) {
		this(descriptor.getUnderlyingName(), descriptor.getMaturity(), descriptor.getStrike());
	}

	public EuropeanOption(String underlyingName, double maturity, double strike) {
		super();
		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strike = strike;
	}

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 */
	public EuropeanOption(double maturity, double strike) {
		this(null, maturity, strike);
	}

	@Override
	public Complex apply(Complex argument) {
		Complex iargument = argument.multiply(Complex.I);
		Complex exponent = (iargument).add(1);
		Complex numerator = (new Complex(strike)).pow(exponent);
		Complex denominator = (argument.multiply(argument)).subtract(iargument);	

		return numerator.divide(denominator).negate();
	}

	@Override
	public double getMaturity() {
		return maturity;
	}

	@Override
	public double getIntegrationDomainImagLowerBound() {
		return 0.5;
	}

	@Override
	public double getIntegrationDomainImagUpperBound() {
		return 2.5;
	}

	@Override
	public SingleAssetEuropeanOptionProductDescriptor getDescriptor() {
		return new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturity, strike);
	}

	@Override
	public Double getValue(double evaluationTime, Model<?> model) {
		Double value = null;
		try {
			value = super.getValue((ProcessCharacteristicFunctionInterface) model);
		} catch (CalculationException e) {
		}
		
		return value;
	}

	/* (non-Javadoc)
	 * @see net.finmath.modelling.Product#getValues(double, net.finmath.modelling.Model)
	 */
	@Override
	public Map<String, Object> getValues(double evaluationTime, Model<?> model) {
		Map<String, Object>  result = new HashMap<String, Object>();

		try {
			double value = super.getValue((ProcessCharacteristicFunctionInterface) model);
			result.put("value", value);
		} catch (CalculationException e) {
			result.put("exception", e);
		}
		
		return result;
	}
}
