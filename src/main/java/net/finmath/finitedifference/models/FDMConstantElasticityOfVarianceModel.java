package net.finmath.finitedifference.models;


import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.solvers.FDMThetaMethod;

/**
 * CEV model using finite difference method.
 *
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jörg Kienitz
 */

public class FDMConstantElasticityOfVarianceModel implements FiniteDifference1DModel{
	private final double initialValue;
	private final double riskFreeRate;
	private final double volatility;
	private final double exponent;

	/*
	 * Solver properties - will be moved to solver.
	 */
	private final int numTimesteps;
	private final int numSpacesteps;
	private final int numStandardDeviations;
	private final double center;
	private final double theta;

	public FDMConstantElasticityOfVarianceModel(
			int numTimesteps,
			int numSpacesteps,
			int numStandardDeviations,
			double center,
			double theta,
			double initialValue,
			double riskFreeRate,
			double volatility,
			double exponent) {
		this.initialValue = initialValue;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
		this.exponent = exponent;

		this.numTimesteps = numTimesteps;
		this.numSpacesteps = numSpacesteps;
		this.numStandardDeviations = numStandardDeviations;
		this.center = center;
		this.theta = theta;
	}

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#varianceOfStockPrice(double)
	 * @TODO Implement correct stock price variance for CEV model. Currently using BS variance.
	 */
	@Override
	public double varianceOfStockPrice(double time) {
		return Math.pow(initialValue, 2) * Math.exp(2 * riskFreeRate * time)
				* (Math.exp(Math.pow(volatility, 2) * time) - 1);
	}

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#getForwardValue(double)
	 */
	@Override
	public double getForwardValue(double time) {
		return initialValue * Math.exp(riskFreeRate * time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#getRiskFreeRate()
	 */
	@Override
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	public double getInitialValue() {
		return initialValue;
	}

	@Override
	public double getVolatility() {
		return volatility;
	}

	@Override
	public double getLocalVolatility(double assetValue, double time) {
		return volatility * Math.pow(assetValue, exponent - 1);
	}

	@Override
	public int getNumTimesteps() {
		return numTimesteps;
	}

	@Override
	public int getNumSpacesteps() {
		return numSpacesteps;
	}

	@Override
	public double getNumStandardDeviations() {
		return numStandardDeviations;
	}

	/* (non-Javadoc)
	 * @see net.finmath.finitedifference.models.FiniteDifference1DModel#valueOptionWithThetaMethod(net.finmath.finitedifference.products.FDMEuropeanCallOption, double)
	 */
	@Override
	public double[][] getValue(double evaluationTime, double time, DoubleUnaryOperator values, FiniteDifference1DBoundary boundary) {
		final FDMThetaMethod solver = new FDMThetaMethod(this, boundary, time, center, theta);
		return solver.getValue(evaluationTime, time, values);
	}

}
