/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2018
 */

package net.finmath.experimental.model;

/**
 * Interface for a describable model.
 * For a description of the general concept see <a href="http://finmath.net/finmath-lib/concepts/separationofproductandmodel">http://finmath.net/finmath-lib/concepts/separationofproductandmodel</a>.
 * 
 * @author Christian Fries
 */
public interface Model<T extends ModelDescriptor> {

	/**
	 * Return a model descriptor representing this model.
	 * 
	 * @return The model descriptor of this model.
	 */
	T getDescriptor();
	

	/**
	 * Construct a product from a product descriptor, which may be valued by this mmodel.
	 * 
	 * @param productDescriptor Given product descriptor.
	 * @return An instance of a product implementation.
	 */
	Product<?> getProductFromDesciptor(ProductDescriptor productDescriptor);
}
