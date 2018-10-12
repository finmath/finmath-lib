/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling.productfactory;

import java.time.LocalDate;

import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.ProductDescriptor;
import net.finmath.modelling.ProductFactory;
import net.finmath.modelling.describedproducts.EuropeanOptionMonteCarlo;
import net.finmath.modelling.descriptor.SingleAssetEuropeanOptionProductDescriptor;

/**
 * @author Christian Fries
 */
public class SingleAssetEuropeanOptionMonteCarloProductFactory implements ProductFactory<SingleAssetEuropeanOptionProductDescriptor> {

	LocalDate referenceDate;
	
	/**
	 * Create factory.
	 */
	public SingleAssetEuropeanOptionMonteCarloProductFactory(LocalDate referenceDate) {
	}

	@Override
	public DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> getProductFromDescriptor(ProductDescriptor descriptor) {

		DescribedProduct<SingleAssetEuropeanOptionProductDescriptor> product = new EuropeanOptionMonteCarlo((SingleAssetEuropeanOptionProductDescriptor) descriptor, referenceDate);
		return product;
	}
}
