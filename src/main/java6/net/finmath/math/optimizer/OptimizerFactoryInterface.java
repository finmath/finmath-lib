/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 29.05.2015
 */

package net.finmath.math.optimizer;

import net.finmath.math.optimizer.OptimizerInterface.ObjectiveFunction;

/**
 * @author Christian Fries
 *
 */
public interface OptimizerFactoryInterface {

	public OptimizerInterface getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] targetValues);
	public OptimizerInterface getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound,double[]  upperBound, double[] targetValues);
	public OptimizerInterface getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound,double[]  upperBound, double[] parameterStep, double[] targetValues);
}
