package net.finmath.finitedifference.models;

import java.util.function.DoubleUnaryOperator;

import net.finmath.finitedifference.solvers.FDMThetaMethod;

/**
 * Black Scholes model using finite difference method.
 *
 * @author Ralph Rudd
 * @author Christian Fries
 * @author Jörg Kienitz
 * @version 1.0
 */
public class FDMBlackScholesModel implements FiniteDifference1DModel {
	private final double initialValue;
	private final double riskFreeRate;
	private final double volatility;

	/*
	 * Solver properties - will be moved to solver.
	 */
	private final int numTimesteps;
	private final int numSpacesteps;
	private final int numStandardDeviations;
	private final double center;
	private final double theta;

	public FDMBlackScholesModel(
			final int numTimesteps,
			final int numSpacesteps,
			final int numStandardDeviations,
			final double center,
			final double theta,
			final double initialValue,
			final double riskFreeRate,
			final double volatility) {
		this.initialValue = initialValue;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;

		this.numTimesteps = numTimesteps;
		this.numSpacesteps = numSpacesteps;
		this.numStandardDeviations = numStandardDeviations;
		this.center = center;
		this.theta = theta;
	}

	@Override
	public double varianceOfStockPrice(final double time) {
		return Math.pow(initialValue, 2) * Math.exp(2 * riskFreeRate * time)
				* (Math.exp(Math.pow(volatility, 2) * time) - 1);
	}

	@Override
	public double getForwardValue(final double time) {
		return initialValue * Math.exp(riskFreeRate * time);
	}

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
		return volatility;
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

	@Override
	public double[][] getValue(final double evaluationnTime, final double time, final DoubleUnaryOperator values, final FiniteDifference1DBoundary boundary) {
		final FDMThetaMethod solver = new FDMThetaMethod(this, boundary, time, center, theta);
		return solver.getValue(evaluationnTime, time, values);
	}

}
