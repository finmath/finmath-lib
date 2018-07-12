/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.productfactory;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.SingleAssetProductDescriptor;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;

/**
 * @author Christian Fries
 */
public class SingleAssetFourierProductFactory implements ProductFactory<SingleAssetProductDescriptor> {

	/**
	 * Create factory.
	 */
	public SingleAssetFourierProductFactory() {
	}
	
	@Override
	public DescribedProduct<? extends SingleAssetProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {

		if(descriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {
			final DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> product = new net.finmath.fouriermethod.products.EuropeanOption((SingleAssetEuropeanOptionProductDescriptor) descriptor);
			return product;
		}
		else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}

	}

}
