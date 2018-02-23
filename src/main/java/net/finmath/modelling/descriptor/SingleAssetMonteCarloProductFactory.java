/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.descriptor;

import net.finmath.modelling.Product;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.SingleAssetProductDescriptor;

/**
 * @author Christian Fries
 */
public class SingleAssetMonteCarloProductFactory implements ProductFactory<SingleAssetProductDescriptor> {

	@Override
	public Product<? extends SingleAssetProductDescriptor> getProductFromDescription(SingleAssetProductDescriptor descriptor) {

		if(descriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {
			Product<SingleAssetEuropeanOptionProductDescriptor> product = new net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption((SingleAssetEuropeanOptionProductDescriptor) descriptor);
			return product;
		}
		else {
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}
}
