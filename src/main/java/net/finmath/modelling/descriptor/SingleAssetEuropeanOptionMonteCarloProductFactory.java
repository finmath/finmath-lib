/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.descriptor;

import net.finmath.modelling.Product;
import net.finmath.modelling.ProductFactory;

/**
 * @author Christian Fries
 */
public class SingleAssetEuropeanOptionMonteCarloProductFactory implements ProductFactory<SingleAssetEuropeanOptionProductDescriptor> {

	/**
	 * Create factory.
	 */
	public SingleAssetEuropeanOptionMonteCarloProductFactory() {
	}

	@Override
	public Product<SingleAssetEuropeanOptionProductDescriptor> getProductFromDescription(SingleAssetEuropeanOptionProductDescriptor descriptor) {

		Product<SingleAssetEuropeanOptionProductDescriptor> product = new net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption((SingleAssetEuropeanOptionProductDescriptor) descriptor);
		return product;
	}
}
