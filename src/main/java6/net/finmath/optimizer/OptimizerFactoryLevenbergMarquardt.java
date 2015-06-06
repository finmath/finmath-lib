/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.OptimizerInterface.ObjectiveFunction;

/**
 * @author Christian Fries
 */
public class OptimizerFactoryLevenbergMarquardt implements OptimizerFactoryInterface {

	private final int		maxIterations;
	private final double	errorTolerance;
	private final int		maxThreads;

	public OptimizerFactoryLevenbergMarquardt(int maxIterations, double errorTolerance, int maxThreads) {
		super();
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public OptimizerFactoryLevenbergMarquardt(int maxIterations, int maxThreads) {
		this(maxIterations, 0.0, maxThreads);
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
	public OptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound,double[]  upperBound, double[] parameterStep, double[] targetValues) {
		return (new LevenbergMarquardt(
				initialParameters,
				targetValues,
				maxIterations,
				maxThreads)
		{	
			private static final long serialVersionUID = -1628631567190057495L;

			@Override
			public void setValues(double[] parameters, double[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		}).setErrorTolerance(errorTolerance);
	}
}
