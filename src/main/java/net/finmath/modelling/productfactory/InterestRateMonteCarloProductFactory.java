package net.finmath.modelling.productfactory;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.descriptor.InterestRateSwapLegProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateModelDescriptor;

public class InterestRateMonteCarloProductFactory implements ProductFactory<InterestRateProductDescriptor, InterestRateModelDescriptor> {

	@Override
	public boolean supportsProduct(ProductDescriptor descriptor) {
		
		if(descriptor instanceof InterestRateSwapLegProductDescriptor)
			return true;
		else
			return false;
	}
	
	@Override
	public DescribedProduct<? extends InterestRateProductDescriptor> getProductFromDescription(ProductDescriptor descriptor) {
		
		if(descriptor instanceof InterestRateSwapLegProductDescriptor) {
			DescribedProduct<InterestRateSwapLegProductDescriptor> product = 
					new net.finmath.montecarlo.interestrate.products.SwapLeg((InterestRateSwapLegProductDescriptor) descriptor);
			return product;
		} else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

}
