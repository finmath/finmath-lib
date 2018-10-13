package net.finmath.modelling.describedproducts;

import java.time.LocalDate;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

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
