package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FiniteDifference1DModel;

/**
 * Interface one dimensional finite difference products.
 * 
 * @author Christian Fries
 */
public interface FiniteDifference1DProduct {

	double[][] getValue(FiniteDifference1DModel model);

	double getMaturity();
}