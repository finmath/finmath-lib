package net.finmath.modelling.describedproducts;

import java.time.LocalDate;

import net.finmath.fouriermethod.products.EuropeanOption;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

/**
 * Contructing the european option valuation implementation using fourier method from a product descriptor.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public class EuropeanOptionFourierMethod extends EuropeanOption  implements DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> {

	private final SingleAssetEuropeanOptionProductDescriptor descriptor;

	/**
	 * Create the product from a descriptor.
	 *
	 * @param descriptor A descriptor of the product.
	 * @param referenceDate the reference date to be used when converting dates to doubles.
	 */
	public EuropeanOptionFourierMethod(SingleAssetEuropeanOptionProductDescriptor descriptor, LocalDate referenceDate) {
		super(descriptor.getUnderlyingName(), FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
		this.descriptor = descriptor;
	}

	@Override
	public SingleAssetEuropeanOptionProductDescriptor getDescriptor() {
		return descriptor;
	}

}
