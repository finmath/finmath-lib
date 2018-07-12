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
import net.finmath.modelling.descriptor.SingleAssetDigitalOptionProductDescriptor;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;

/**
 * @author Christian Fries
 */
public class SingleAssetMonteCarloProductFactory implements ProductFactory<SingleAssetProductDescriptor> {

	@Override
	public DescribedProduct<? extends SingleAssetProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {

		if(descriptor instanceof SingleAssetEuropeanOptionProductDescriptor) {
			DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> product = new net.finmath.montecarlo.assetderivativevaluation.products.EuropeanOption((SingleAssetEuropeanOptionProductDescriptor) descriptor);
			return product;
		} else if(descriptor instanceof SingleAssetDigitalOptionProductDescriptor) {
			DescribedProduct<SingleAssetDigitalOptionProductDescriptor> product = new net.finmath.montecarlo.assetderivativevaluation.products.DigitalOption((SingleAssetDigitalOptionProductDescriptor) descriptor);
			return product;
		}else{
			String name = descriptor.name();
			throw new IllegalArgumentException("Unsupported product type " + name);
		}
	}

}
