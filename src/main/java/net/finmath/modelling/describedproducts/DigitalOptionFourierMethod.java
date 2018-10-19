package net.finmath.modelling.describedproducts;

import java.time.LocalDate;

import net.finmath.fouriermethod.products.DigitalOption;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

/**
 * Fourier method based implementation of a digital option from a product descriptor.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public class DigitalOptionFourierMethod extends DigitalOption  implements DescribedProduct<SingleAssetDigitalOptionProductDescriptor>{

	private final SingleAssetDigitalOptionProductDescriptor descriptor;

	/**
	 * Create product from descriptor.
	 * 
	 * @param descriptor The descriptor of the product.
	 * @param referenceDate The reference date of the data for the valuation, used to convert absolute date to relative dates in double representation.
	 */
	public DigitalOptionFourierMethod(SingleAssetDigitalOptionProductDescriptor descriptor, LocalDate referenceDate) {
		super(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
		this.descriptor = descriptor;
	}

	@Override
	public SingleAssetDigitalOptionProductDescriptor getDescriptor() {
		return descriptor;
	}

}
