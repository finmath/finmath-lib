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
public interface OptimizerFactoryInterface {

	public OptimizerInterface getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound,double[]  upperBound, double[] parameterStep, double[] targetValues);
}
