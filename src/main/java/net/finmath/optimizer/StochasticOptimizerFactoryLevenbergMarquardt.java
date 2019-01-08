/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christianfries.com.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.StochasticOptimizerInterface.ObjectiveFunction;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * @author Christian Fries
 * @version 1.0
 */
public class StochasticOptimizerFactoryLevenbergMarquardt implements StochasticOptimizerFactoryInterface {

	private final StochasticLevenbergMarquardt.RegularizationMethod regularizationMethod;
	private final int		maxIterations;
	private final double		errorTolerance;
	private final int		maxThreads;

	public StochasticOptimizerFactoryLevenbergMarquardt(StochasticLevenbergMarquardt.RegularizationMethod regularizationMethod, int maxIterations, double errorTolerance, int maxThreads) {
		super();
		this.regularizationMethod = regularizationMethod;
		this.maxIterations = maxIterations;
		this.errorTolerance = errorTolerance;
		this.maxThreads = maxThreads;
	}

	public StochasticOptimizerFactoryLevenbergMarquardt(int maxIterations, double errorTolerance, int maxThreads) {
		this(StochasticLevenbergMarquardt.RegularizationMethod.LEVENBERG_MARQUARDT, maxIterations, errorTolerance, maxThreads);
	}

	public StochasticOptimizerFactoryLevenbergMarquardt(int maxIterations, int maxThreads) {
		this(maxIterations, 0.0, maxThreads);
	}

	@Override
	public StochasticOptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, RandomVariableInterface[] initialParameters, RandomVariableInterface[] lowerBound, RandomVariableInterface[]  upperBound, RandomVariableInterface[] parameterSteps, RandomVariableInterface[] targetValues) {
		return
				new StochasticLevenbergMarquardt(regularizationMethod, initialParameters, targetValues, parameterSteps, maxIterations, errorTolerance, maxThreads)
		{
			private static final long serialVersionUID = -7050719719557572792L;

			@Override
			public void setValues(RandomVariableInterface[] parameters, RandomVariableInterface[] values) throws SolverException {
				objectiveFunction.setValues(parameters, values);
			}
		};
	}
}
