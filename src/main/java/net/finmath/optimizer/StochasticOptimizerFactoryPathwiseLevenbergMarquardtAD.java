/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
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
public class StochasticOptimizerFactoryPathwiseLevenbergMarquardtAD implements StochasticOptimizerFactoryInterface {

	private final int		maxIterations;
	private final RandomVariable		errorTolerance;
	private final int		maxThreads;

	public StochasticOptimizerFactoryPathwiseLevenbergMarquardtAD(int maxIterations, RandomVariable errorTolerance, int maxThreads) {
		super();
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public StochasticOptimizerFactoryPathwiseLevenbergMarquardtAD(int maxIterations, int maxThreads) {
		this(maxIterations, null, maxThreads);
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
		return new StochasticPathwiseLevenbergMarquardtAD(initialParameters, targetValues, null, null, maxIterations, errorTolerance, null)
		{
			private static final long serialVersionUID = -4802903981061716810L;

			@Override
			public void setValues(RandomVariable[] parameters, RandomVariable[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		};
	}
}
