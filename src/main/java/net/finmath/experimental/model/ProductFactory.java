/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 09.02.2018
 */

package net.finmath.experimental.model;

/**
 * 
 * @author Christian Fries
 * @author Luca Del Re
 */
public interface ProductFactory<T extends ProductDescriptor> {

	/**
	 * Constructs the product from a given product descriptor.
	 * 
	 * @param description A product descriptor.
	 * @return An instance of the product describable by this descriptor.
	 */
	Product<?> getProductFromDescription(T description);
}
