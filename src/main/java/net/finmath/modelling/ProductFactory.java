/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.modelling;

/**
 * 
 * @author Christian Fries
 * @author Luca Del Re
 */
public interface ProductFactory<P extends ProductDescriptor> {

	/**
	 * Constructs the product from a given product descriptor.
	 * 
	 * @param descriptor A product descriptor.
	 * @return An instance of the product describable by this descriptor.
	 */
	DescribedProduct<? extends P> getProductFromDescriptor(ProductDescriptor descriptor);
}
