/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.StochasticOptimizerInterface.ObjectiveFunction;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class StochasticPathwiseOptimizerFactoryLevenbergMarquardt implements StochasticOptimizerFactoryInterface {

	private final int		maxIterations;
	private final double	errorTolerance;
	private final int		maxThreads;

	public StochasticPathwiseOptimizerFactoryLevenbergMarquardt(int maxIterations, double errorTolerance, int maxThreads) {
		super();
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public StochasticPathwiseOptimizerFactoryLevenbergMarquardt(int maxIterations, int maxThreads) {
		this(maxIterations, 0.0, maxThreads);
	}

	@Override
	public StochasticOptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, RandomVariable[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, null, null, null, targetValues);
	}

	@Override
	public StochasticOptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, final RandomVariable[] lowerBound, final RandomVariable[]  upperBound, RandomVariable[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, lowerBound, upperBound, null, targetValues);
	}

	@Override
	public StochasticOptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, RandomVariable[] initialParameters, RandomVariable[] lowerBound, RandomVariable[]  upperBound, RandomVariable[] parameterSteps, RandomVariable[] targetValues) {
		return
				new StochasticPathwiseLevenbergMarquardt(initialParameters, targetValues, null /* weights */, parameterSteps, maxIterations, null, null)
		{
			private static final long serialVersionUID = -7050719719557572792L;

			@Override
			public void setValues(RandomVariable[] parameters, RandomVariable[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		};
	}
}
