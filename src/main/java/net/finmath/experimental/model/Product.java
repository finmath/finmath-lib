/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
	 * Valuation of this product under a given model.
	 * 
	 * @param model The model under which the product will be valued.
	 * @return Result map.
	 */
	Map<String, Object> getValue(Model<?> model);
}
