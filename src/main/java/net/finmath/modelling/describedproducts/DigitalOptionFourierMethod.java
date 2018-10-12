package net.finmath.modelling.describedproducts;

import java.time.LocalDate;

import net.finmath.fouriermethod.products.DigitalOption;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.time.FloatingpointDate;

public class DigitalOptionFourierMethod extends DigitalOption  implements DescribedProduct<SingleAssetDigitalOptionProductDescriptor>{
	
	private final SingleAssetDigitalOptionProductDescriptor descriptor;

	public DigitalOptionFourierMethod(SingleAssetDigitalOptionProductDescriptor descriptor, LocalDate referenceDate) {
		super(FloatingpointDate.getFloatingPointDateFromDate(referenceDate, descriptor.getMaturity()), descriptor.getStrike());
		this.descriptor = descriptor;
	}

	@Override
	public SingleAssetDigitalOptionProductDescriptor getDescriptor() {
		return descriptor;
	}

}
