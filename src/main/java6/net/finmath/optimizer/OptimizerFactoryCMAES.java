/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 30.05.2015
 */

package net.finmath.optimizer;

import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.random.MersenneTwister;

import net.finmath.optimizer.OptimizerInterface.ObjectiveFunction;

/**
 * @author Christian Fries
 *
 */
public class OptimizerFactoryCMAES implements OptimizerFactoryInterface {

	private final double	accuracy;
	private final int		maxIterations;
	private final double[]	parameterLowerBound;
	private final double[]	parameterUppderBound;
	private final double[]	parameterStandardDeviation;

	public OptimizerFactoryCMAES(double accuracy, int maxIterations,
			double[] parameterLowerBound, double[] parameterUppderBound,
			double[] parameterStandardDeviation) {
		super();
		this.accuracy = accuracy;
		this.maxIterations = maxIterations;
		this.parameterLowerBound = parameterLowerBound;
		this.parameterUppderBound = parameterUppderBound;
		this.parameterStandardDeviation = parameterStandardDeviation;
	}

	public OptimizerFactoryCMAES(double accuracy, int maxIterations, double[] parameterStandardDeviation) {
		super();
		this.accuracy = accuracy;
		this.maxIterations = maxIterations;
		this.parameterLowerBound = null;
		this.parameterUppderBound = null;
		this.parameterStandardDeviation = parameterStandardDeviation;
	}

	public OptimizerFactoryCMAES(double accuracy, int maxIterations) {
		super();
		this.accuracy = accuracy;
		this.maxIterations = maxIterations;
		this.parameterLowerBound = null;
		this.parameterUppderBound = null;
		this.parameterStandardDeviation = null;
	}

	@Override
	public OptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, final double[] initialParameters) {
		return getOptimizer(objectiveFunction, initialParameters, null, null, null, null);
	}

	@Override
	public OptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, final double[] initialParameters, final double[] lowerBound,final double[]  upperBound, double[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, lowerBound, upperBound, null, targetValues);
	}

	@Override
	public OptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, final double[] initialParameters, final double[] lowerBound,final double[]  upperBound, final double[] parameterStep, double[] targetValues) {
		final double[] values = new double[targetValues.length];
		final double[] effectiveParameterLowerBound			= parameterLowerBound != null ? this.parameterLowerBound : lowerBound;
		final double[] effectiveParameterUpperBound			= parameterUppderBound != null ? this.parameterUppderBound : upperBound;
		final double[] effectiveParameterStandardDeviation	= parameterStandardDeviation != null ? this.parameterStandardDeviation : parameterStep;

		return new OptimizerInterface() {

			private org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer optimizer;
			private org.apache.commons.math3.optim.PointValuePair result;

			@Override
			public double[] getBestFitParameters() {
				return result.getPoint();
			}

			@Override
			public double getRootMeanSquaredError() {
				return result.getValue();
			}

			@Override
			public int getIterations() {
				return optimizer != null ? optimizer.getIterations() : 0;
			}

			@Override
			public void run() throws SolverException {
				optimizer = new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer(maxIterations, accuracy, true, 0, 0, new MersenneTwister(3141), false, new SimplePointChecker<org.apache.commons.math3.optim.PointValuePair>(0, 0)) {
					@Override
					public double computeObjectiveValue(double[] parameters) {
						try {
							objectiveFunction.setValues(parameters, values);
						} catch (SolverException e) {
							return Double.NaN;
						}
						double rms = 0;
						for(double value : values) rms += value*value;
						return Math.sqrt(rms);
					}			

					@Override
					public org.apache.commons.math3.optim.nonlinear.scalar.GoalType getGoalType() {
						return org.apache.commons.math3.optim.nonlinear.scalar.GoalType.MINIMIZE;
					}

					@Override
					public double[] getStartPoint() {
						return initialParameters;
					}

					@Override
					public double[] getLowerBound() {
						return effectiveParameterLowerBound;
					}

					@Override
					public double[] getUpperBound() {
						return effectiveParameterUpperBound;
					}
				};

				try {
					result = optimizer.optimize(
							new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.PopulationSize((int) (4 + 3 * Math.log((double)initialParameters.length))),
							new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.Sigma(effectiveParameterStandardDeviation)
							);
				} catch(org.apache.commons.math3.exception.MathIllegalStateException e) {
					new SolverException(e);
				}
			}
		};
	}
}