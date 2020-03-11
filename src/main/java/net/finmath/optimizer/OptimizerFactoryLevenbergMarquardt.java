/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.Optimizer.ObjectiveFunction;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class OptimizerFactoryLevenbergMarquardt implements OptimizerFactory {

	private final LevenbergMarquardt.RegularizationMethod regularizationMethod;
	private final double	lambda;
	private final int		maxIterations;
	private final double	errorTolerance;
	private final int		maxThreads;

	public OptimizerFactoryLevenbergMarquardt(final LevenbergMarquardt.RegularizationMethod regularizationMethod, final double lambda, final int maxIterations, final double errorTolerance, final int maxThreads) {
		super();
		this.regularizationMethod = regularizationMethod;
		this.lambda = lambda;
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public OptimizerFactoryLevenbergMarquardt(final LevenbergMarquardt.RegularizationMethod regularizationMethod, final int maxIterations, final double errorTolerance, final int maxThreads) {
		this(regularizationMethod, 0.001, maxIterations, errorTolerance, maxThreads);
	}

	public OptimizerFactoryLevenbergMarquardt(final int maxIterations, final double errorTolerance, final int maxThreads) {
		this(LevenbergMarquardt.RegularizationMethod.LEVENBERG_MARQUARDT, maxIterations, errorTolerance, maxThreads);
	}

	public OptimizerFactoryLevenbergMarquardt(final int maxIterations, final int maxThreads) {
		this(maxIterations, 0.0, maxThreads);
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
	public Optimizer getOptimizer(final ObjectiveFunction objectiveFunction, final double[] initialParameters, final double[] lowerBound,final double[]  upperBound, final double[] parameterSteps, final double[] targetValues) {
		return (new LevenbergMarquardt(
				regularizationMethod,
				initialParameters,
				targetValues,
				maxIterations,
				maxThreads)
		{
			private static final long serialVersionUID = -1628631567190057495L;

			@Override
			public void setValues(final double[] parameters, final double[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		})
				.setLambda(lambda)
				.setErrorTolerance(errorTolerance)
				.setParameterSteps(parameterSteps);
	}
}
