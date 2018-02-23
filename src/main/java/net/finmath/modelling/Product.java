/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 11.10.2013
 */

package net.finmath.modelling;

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
	default T getDescriptor() { throw new UnsupportedOperationException(); }

	/**
	 * Return the valuation of the product using the given model.
	 * 
	 * Implement this method using a checked cast of the model to a derived model for which the product
	 * provides a valuation algorithm. Example: an interest rate product requires that the passed model
	 * object implements the interface of an interest rate model. Since there is no polymorphism on
	 * arguments (see Double Dynamic Dispatch), we reply on a checked cast.
	 * 
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param model The model under which the product is valued.
	 * @return The value of the product using the given model.
	 */
	Object getValue(double evaluationTime, Model<?> model);

	/**
	 * Return the valuation of the product using the given model.
	 * 
	 * Implement this method using a checked cast of the model to a derived model for which the product
	 * provides a valuation algorithm. Example: an interest rate product requires that the passed model
	 * object implements the interface of an interest rate model. Since there is no polymorphism on
	 * arguments (see Double Dynamic Dispatch), we reply on a checked cast.
	 * 
	 * @param evaluationTime The evaluation time as double. Cash flows prior and including this time are not considered.
	 * @param model The model under which the product is valued.
	 * @return Map containing the value of the product using the given model.
	 */
	default Map<String, Object> getValues(double evaluationTime, Model<?> model) { throw new UnsupportedOperationException(); };
}
