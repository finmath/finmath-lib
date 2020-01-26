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
public class StochasticOptimizerFactoryLevenbergMarquardt implements StochasticOptimizerFactory {

	private final StochasticLevenbergMarquardt.RegularizationMethod regularizationMethod;
	private final int		maxIterations;
	private final double		errorTolerance;
	private final int		maxThreads;

	public StochasticOptimizerFactoryLevenbergMarquardt(final StochasticLevenbergMarquardt.RegularizationMethod regularizationMethod, final int maxIterations, final double errorTolerance, final int maxThreads) {
		super();
		this.regularizationMethod = regularizationMethod;
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public StochasticOptimizerFactoryLevenbergMarquardt(final int maxIterations, final double errorTolerance, final int maxThreads) {
		this(StochasticLevenbergMarquardt.RegularizationMethod.LEVENBERG_MARQUARDT, maxIterations, errorTolerance, maxThreads);
	}

	public StochasticOptimizerFactoryLevenbergMarquardt(final int maxIterations, final int maxThreads) {
		this(maxIterations, 0.0, maxThreads);
	}

	@Override
	public StochasticOptimizer getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, final RandomVariable[] lowerBound, final RandomVariable[]  upperBound, final RandomVariable[] parameterSteps, final RandomVariable[] targetValues) {
		return
				new StochasticLevenbergMarquardt(regularizationMethod, initialParameters, targetValues, parameterSteps, maxIterations, errorTolerance, maxThreads)
		{
			private static final long serialVersionUID = -7050719719557572792L;

			@Override
			public void setValues(final RandomVariable[] parameters, final RandomVariable[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		};
	}
}
