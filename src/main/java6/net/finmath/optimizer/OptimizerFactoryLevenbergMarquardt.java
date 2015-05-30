/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.OptimizerInterface.ObjectiveFunction;

/**
 * @author Christian Fries
 *
 */
public class OptimizerFactoryLevenbergMarquardt implements OptimizerFactoryInterface {

	private final int		maxIterations;
	private final int		maxThreads;
	

	public OptimizerFactoryLevenbergMarquardt(int maxIterations, int maxThreads) {
		super();
		this.maxIterations = maxIterations;
		this.maxThreads = maxThreads;
	}

	@Override
	public OptimizerInterface getOptimizer(final ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound,double[]  upperBound, double[] parameterStep, double[] targetValues) {
		return new LevenbergMarquardt(
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
		};
	}
}
