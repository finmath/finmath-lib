package net.finmath.finitedifference.products;

import net.finmath.finitedifference.models.FiniteDifference1DModel;

public interface FiniteDifference1DProduct {

	double[][] getValue(FiniteDifference1DModel model);

	double getMaturity();
}