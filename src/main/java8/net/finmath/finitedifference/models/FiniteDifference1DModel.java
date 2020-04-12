package net.finmath.finitedifference.models;

import java.util.function.DoubleUnaryOperator;

import net.finmath.modelling.Model;

/**
 * Interface one dimensional finite difference models.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface FiniteDifference1DModel extends Model {

	/**
	 * Return the conditional expectation of the given values at a given time contrained by the given boundary conditions.
	 *
	 * @param evaluationTime The time at which the conditional expectation is requested.
	 * @param time The time at which we observe values.
	 * @param values The values.
	 * @param boundary The given boundary conditions
	 * @return Vector of { states , values }.
	 */
	double[][] getValue(double evaluationTime, double time, DoubleUnaryOperator values, FiniteDifference1DBoundary boundary);

	double varianceOfStockPrice(double time);

	double getForwardValue(double time);

	double getRiskFreeRate();

	double getNumStandardDeviations();

	int getNumSpacesteps();

	int getNumTimesteps();

	double getVolatility();

	double getLocalVolatility(double assetValue, double time);
}
