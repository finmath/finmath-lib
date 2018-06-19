package net.finmath.finitedifference.models;

import net.finmath.finitedifference.products.FDMEuropeanCallOption;

public interface FiniteDifference1DModel {

	double varianceOfStockPrice(double time);

	double getForwardValue(double time);

	double getRiskFreeRate();

	double[][] valueOptionWithThetaMethod(FDMEuropeanCallOption option);
}