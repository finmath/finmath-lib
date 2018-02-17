/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 09.02.2018
 */

package net.finmath.experimental.model;

import java.util.Map;

/**
 * Interface for a describable product.
 * For a description of the general concept see <a href="http://finmath.net/finmath-lib/concepts/separationofproductandmodel">http://finmath.net/finmath-lib/concepts/separationofproductandmodel</a>.
 * 
 * @author Christian Fries
 */
public interface Product<T extends ProductDescriptor> {

	/**
	 * Return a product descriptor representing this model.
	 * 
	 * @return The product descriptor of this product.
	 */
	T getDescriptor();

	/**
	 * @param model The model under which the product will be valued.
	 * @return Result map.
	 */
	Map<String, Object> getValue(Model<?> model);
}
