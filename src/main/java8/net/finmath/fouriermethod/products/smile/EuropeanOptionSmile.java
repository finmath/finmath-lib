package net.finmath.fouriermethod.products.smile;

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

	public EuropeanOptionSmile(final String underlyingName, final double maturity, final double[] strikes) {
		super();
		this.underlyingName = underlyingName;
		this.maturity = maturity;
		this.strikes = strikes;
	}

	public EuropeanOptionSmile(final double maturity, final double[] strikes) {
		this(null,maturity,strikes);
	}

	@Override
	public double getMaturity() {
		return maturity;
	}

	public double[] getStrikes() {
		return strikes;
	}

	public String getUnderlyingName() {
		return underlyingName;
	}

	@Override
	public double getIntegrationDomainImagLowerBound() {
		return 0;
	}

	@Override
	public double getIntegrationDomainImagUpperBound() {
		return -1;
	}

	@Override
	public abstract Map<String, Function<Double,Double>> getValue(double evaluationTime, CharacteristicFunctionModel model) throws CalculationException;

	/**
	 * Returns the same valuation method for different parameters (maturity and strikes).
	 *
	 * @param maturity The new maturity.
	 * @param strikes The new strikes.
	 * @return the same valuation method now referring to a different maturity and strike grid.
	 */
	public abstract EuropeanOptionSmile getCloneWithModifiedParameters(double maturity, double[] strikes);


	@Override
	public Complex apply(final Complex z) {
		return ((z.subtract(Complex.I)).multiply(z)).negate();
	}

	/**
	 * Return a collection of product descriptors for each option in the smile.
	 *
	 * @param referenceDate The reference date (translating the maturity floating point date to dates.
	 * @return a collection of product descriptors for each option in the smile.
	 */
	public Map<Double, SingleAssetEuropeanOptionProductDescriptor> getDescriptors(final LocalDate referenceDate){

		final int numberOfStrikes = strikes.length;
		final HashMap<Double, SingleAssetEuropeanOptionProductDescriptor> descriptors = new HashMap<>();

		final LocalDate maturityDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, maturity);
		for(int i = 0; i< numberOfStrikes; i++) {
			descriptors.put(strikes[i], new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturityDate, strikes[i]));
		}

		return descriptors;
	}

	/**
	 * Return a product descriptor for a specific strike.
	 *
	 * @param referenceDate The reference date (translating the maturity floating point date to dates.
	 * @param index The index corresponding to the strike grid.
	 * @return a product descriptor for a specific strike.
	 * @throws ArrayIndexOutOfBoundsException Thrown if index is out of bound.
	 */
	public SingleAssetEuropeanOptionProductDescriptor getDescriptor(final LocalDate referenceDate, final int index) throws ArrayIndexOutOfBoundsException{
		final LocalDate maturityDate = FloatingpointDate.getDateFromFloatingPointDate(referenceDate, maturity);
		if(index >= strikes.length) {
			throw new ArrayIndexOutOfBoundsException("Strike index out of bounds");
		}else {
			return new SingleAssetEuropeanOptionProductDescriptor(underlyingName, maturityDate, strikes[index]);
		}
	}

}
