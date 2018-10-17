package net.finmath.modelling.describedproducts;

import java.time.LocalDate;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

/**
 * Monte-Carlo method based implementation of a digital option from a product descriptor.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public class DigitalOptionMonteCarlo extends net.finmath.montecarlo.assetderivativevaluation.products.DigitalOption
implements DescribedProduct<SingleAssetDigitalOptionProductDescriptor>  {

	private final SingleAssetDigitalOptionProductDescriptor descriptor;

	/**
	 * Create product from descriptor.
	 * 
	 * @param descriptor The descriptor of the product.
	 * @param referenceDate The reference date of the data for the valuation, used to convert absolute date to relative dates in double representation.
	 */
	public DigitalOptionMonteCarlo(SingleAssetDigitalOptionProductDescriptor descriptor, LocalDate referenceDate) {
		super(descriptor.getNameOfUnderlying(), FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
		this.descriptor = descriptor;
	}

	@Override
	public SingleAssetDigitalOptionProductDescriptor getDescriptor() {
		return descriptor;
	}
}
