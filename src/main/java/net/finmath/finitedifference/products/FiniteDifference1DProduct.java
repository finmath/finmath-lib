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
	 * @param model
	 * @return
	 */
	double[][] getValue(double evaluationTime, FiniteDifference1DModel model);
}