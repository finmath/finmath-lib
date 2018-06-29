package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FiniteDifference1DModel;

/**
 * Interface one dimensional finite difference products.
 * 
 * @author Christian Fries
 */
public interface FiniteDifference1DProduct {

	/**
	 * Return the value of the product under the given model.
	 * 
	 * @param evaluationTime The time at which the value (valuation) is requested.
	 * @param model The model under which the valuation should be performed.
	 * @return The random variable representing the valuation result.
	 */
	double[][] getValue(double evaluationTime, FiniteDifference1DModel model);
}