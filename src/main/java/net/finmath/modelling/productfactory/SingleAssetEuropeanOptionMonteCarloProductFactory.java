/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.productfactory;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.descriptor.AssetModelDescriptor;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;

/**
 * @author Christian Fries
 */
public class SingleAssetEuropeanOptionMonteCarloProductFactory implements ProductFactory<SingleAssetEuropeanOptionProductDescriptor, AssetModelDescriptor> {

	/**
	 * Create factory.
	 */
	public SingleAssetEuropeanOptionMonteCarloProductFactory() {
	}

	@Override
	public DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> getProductFromDescription(ProductDescriptor descriptor) {

		DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> product = new net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption((SingleAssetEuropeanOptionProductDescriptor) descriptor);
		return product;
	}

	@Override
	public boolean supportsProduct(ProductDescriptor descriptor) {
		if(descriptor instanceof SingleAssetEuropeanOptionProductDescriptor)
			return true;
		else
			return false;
	}
}
