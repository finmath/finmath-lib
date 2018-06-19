package net.finmath.finitedifference.models;

import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.products.FDMEuropeanCallOption;

public interface FiniteDifference1DModel {

	double[][] getValue(double time, DoubleUnaryOperator values, FiniteDifference1DBoundary boundary);

	double varianceOfStockPrice(double time);

	double getForwardValue(double time);

	double getRiskFreeRate();

	double getNumStandardDeviations();

	int getNumSpacesteps();

	double getVolatility();

}