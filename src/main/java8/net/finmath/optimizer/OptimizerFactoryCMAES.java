/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 30.05.2015
 */

package net.finmath.optimizer;

import org.apache.commons.math3.optim.SimplePointChecker;
import org.apache.commons.math3.random.MersenneTwister;

import net.finmath.optimizer.Optimizer.ObjectiveFunction;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public class OptimizerFactoryCMAES implements OptimizerFactory {

	private final double	accuracy;
	private final int		maxIterations;
	private final double[]	parameterLowerBound;
	private final double[]	parameterUppderBound;
	private final double[]	parameterStandardDeviation;

	public OptimizerFactoryCMAES(final double accuracy, final int maxIterations,
			final double[] parameterLowerBound, final double[] parameterUppderBound,
			final double[] parameterStandardDeviation) {
		super();
		this.accuracy = accuracy;
		this.maxIterations = maxIterations;
		this.parameterLowerBound = parameterLowerBound;
		this.parameterUppderBound = parameterUppderBound;
		this.parameterStandardDeviation = parameterStandardDeviation;
	}

	public OptimizerFactoryCMAES(final double accuracy, final int maxIterations, final double[] parameterStandardDeviation) {
		super();
		this.accuracy = accuracy;
		this.maxIterations = maxIterations;
		parameterLowerBound = null;
		parameterUppderBound = null;
		this.parameterStandardDeviation = parameterStandardDeviation;
	}

	public OptimizerFactoryCMAES(final double accuracy, final int maxIterations) {
		super();
		this.accuracy = accuracy;
		this.maxIterations = maxIterations;
		parameterLowerBound = null;
		parameterUppderBound = null;
		parameterStandardDeviation = null;
	}

	@Override
	public Optimizer getOptimizer(final ObjectiveFunction objectiveFunction, final double[] initialParameters, final double[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, null, null, null, targetValues);
	}

	@Override
	public Optimizer getOptimizer(final ObjectiveFunction objectiveFunction, final double[] initialParameters, final double[] lowerBound,final double[]  upperBound, final double[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, lowerBound, upperBound, null, targetValues);
	}

	@Override
	public Optimizer getOptimizer(final ObjectiveFunction objectiveFunction, final double[] initialParameters, final double[] lowerBound,final double[]  upperBound, final double[] parameterStep, final double[] targetValues) {
		final double[] values = new double[targetValues.length];
		final double[] effectiveParameterLowerBound			= parameterLowerBound != null ? parameterLowerBound : lowerBound;
		final double[] effectiveParameterUpperBound			= parameterUppderBound != null ? parameterUppderBound : upperBound;
		final double[] effectiveParameterStandardDeviation	= parameterStandardDeviation != null ? parameterStandardDeviation : parameterStep;

		// Throw exception if std dev is non null, but lower bound / upper bound are null.
		return new Optimizer() {

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
			public void run() {
				optimizer = new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer(maxIterations, accuracy, true, 0, 0, new MersenneTwister(3141), false, new SimplePointChecker<org.apache.commons.math3.optim.PointValuePair>(0, 0)) {
					@Override
					public double computeObjectiveValue(final double[] parameters) {
						try {
							objectiveFunction.setValues(parameters, values);
						} catch (final SolverException e) {
							return Double.NaN;
						}
						double rms = 0;
						for(final double value : values) {
							rms += value*value;
						}
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
							new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.PopulationSize((int) (4 + 3 * Math.log(initialParameters.length))),
							new org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer.Sigma(effectiveParameterStandardDeviation)
							);
				} catch(final org.apache.commons.math3.exception.MathIllegalStateException e) {
					new SolverException(e);
				}
			}
		};
	}
}
