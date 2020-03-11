/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
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
public class StochasticOptimizerFactoryPathwiseLevenbergMarquardtAD implements StochasticOptimizerFactory {

	private final int		maxIterations;
	private final RandomVariable		errorTolerance;
	private final int		maxThreads;

	public StochasticOptimizerFactoryPathwiseLevenbergMarquardtAD(final int maxIterations, final RandomVariable errorTolerance, final int maxThreads) {
		super();
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public StochasticOptimizerFactoryPathwiseLevenbergMarquardtAD(final int maxIterations, final int maxThreads) {
		this(maxIterations, null, maxThreads);
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
		return new StochasticPathwiseLevenbergMarquardtAD(initialParameters, targetValues, null, null, maxIterations, errorTolerance, null)
		{
			private static final long serialVersionUID = -4802903981061716810L;

			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		};
	}
}
