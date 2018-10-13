package net.finmath.modelling.describedproducts;

import java.time.LocalDate;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

/**
 * Contructing the digital option valuation implementation using Monte-Carlo method from a product descriptor.
 *
 * @author Christian Fries
 * @author Roland Bachl
 */
public class DigitalOptionMonteCarlo extends net.finmath.montecarlo.assetderivativevaluation.products.DigitalOption
implements DescribedProduct<SingleAssetDigitalOptionProductDescriptor>  {

	private final SingleAssetDigitalOptionProductDescriptor descriptor;

	public DigitalOptionMonteCarlo(SingleAssetDigitalOptionProductDescriptor descriptor, LocalDate referenceDate) {
		super(descriptor.getNameOfUnderlying(), FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
		this.descriptor = descriptor;
	}

	@Override
	public SingleAssetDigitalOptionProductDescriptor getDescriptor() {
		return descriptor;
	}
}
