package net.finmath.fouriermethod.products;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.apache.commons.math3.complex.Complex;

import net.finmath.exception.CalculationException;
import net.finmath.fouriermethod.models.CharacteristicFunctionModel;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

/**
 * This is an abstract base class for Fourier-based methodologies for the valuation of a smile of options.
 *
 * Concrete different Fourier methodologies should provide different implementations of the getValue method, which is left
 * here as abstract.
 *
 * @author Alessandro Gnoatto
 *
 */
public abstract class EuropeanOptionSmile implements SmileByIntegralTransform {

	private final String underlyingName;
	private final double maturity;
	private final double[] strikes;

	public EuropeanOptionSmile(String underlyingName, double maturity, double[] strikes) {
		super();
		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strikes = strikes;
	}

	public EuropeanOptionSmile(double maturity, double[] strikes) {
		this(null,maturity,strikes);
	}

	@Override
	public double getMaturity() {
		return 1.0;//this.maturity;
	}

	public double[] getStrikes() {
		return this.strikes;
	}

	public String getUnderlyingName() {
		return this.underlyingName;
	}

	@Override
	public double getIntegrationDomainImagLowerBound() {
		return 0;
	}

	@Override
	public double getIntegrationDomainImagUpperBound() {
		return -1;
	}

	public abstract Map<String, Function<Double,Double>> getValue(double evaluationTime, CharacteristicFunctionModel model) throws CalculationException;

	/**
	 * This method allows us to reuse the same pricer (same pricing algorithm) over different option smiles.
	 * @param maturity
	 * @param strikes
	 * @return the same pricer now referring to a different smile.
	 */
	public abstract EuropeanOptionSmile getCloneWithModifiedParameters(double maturity, double[] strikes);


	@Override
	public Complex apply(Complex z) {
		return ((z.subtract(Complex.I)).multiply(z)).negate();
	}

	/**
	 * Return a collection of product descriptors for each option in the smile.
	 * @return a collection of product descriptors for each option in the smile.
	 */
	public Map<Double, SingleAssetEuropeanOptionProductDescriptor> getDescriptors(LocalDate referenceDate){

		int numberOfStrikes = strikes.length;
		HashMap<Double, SingleAssetEuropeanOptionProductDescriptor> descriptors = new HashMap<Double, SingleAssetEuropeanOptionProductDescriptor>();

		LocalDate maturityDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, maturity);
		for(int i = 0; i< numberOfStrikes; i++) {
			descriptors.put(strikes[i], new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturityDate, strikes[i]));
		}

		return descriptors;
	}

	/**
	 * Return a product descriptor for a specific strike.
	 * @param index
	 * @return a product descriptor for a specific strike.
	 * @throws IllegalArgumentException
	 */
	public SingleAssetEuropeanOptionProductDescriptor getDescriptor(LocalDate referenceDate, int index) throws IllegalArgumentException{
		LocalDate maturityDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, maturity);
		if(index >= strikes.length) {
			throw new IllegalArgumentException("Strike index out of bounds");
		}else {
			return new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturityDate, strikes[index]);
		}
	}

}
