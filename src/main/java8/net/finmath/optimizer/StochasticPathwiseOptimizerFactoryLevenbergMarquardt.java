/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.StochasticOptimizer.ObjectiveFunction;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class StochasticPathwiseOptimizerFactoryLevenbergMarquardt implements StochasticOptimizerFactory {

	private final int		maxIterations;
	private final double	errorTolerance;
	private final int		maxThreads;

	public StochasticPathwiseOptimizerFactoryLevenbergMarquardt(final int maxIterations, final double errorTolerance, final int maxThreads) {
		super();
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public StochasticPathwiseOptimizerFactoryLevenbergMarquardt(final int maxIterations, final int maxThreads) {
		this(maxIterations, 0.0, maxThreads);
	}

	@Override
	public StochasticOptimizer getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, final RandomVariable[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, null, null, null, targetValues);
	}

	@Override
	public StochasticOptimizer getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, final RandomVariable[] lowerBound, final RandomVariable[]  upperBound, final RandomVariable[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, lowerBound, upperBound, null, targetValues);
	}

	@Override
	public StochasticOptimizer getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, final RandomVariable[] lowerBound, final RandomVariable[]  upperBound, final RandomVariable[] parameterSteps, final RandomVariable[] targetValues) {
		return
				new StochasticPathwiseLevenbergMarquardt(initialParameters, targetValues, null /* weights */, parameterSteps, maxIterations, null, null)
		{
			private static final long serialVersionUID = -7050719719557572792L;

			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		};
	}
}
