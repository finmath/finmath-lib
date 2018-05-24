/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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

	OptimizerInterface getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] targetValues);

	OptimizerInterface getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound, double[] upperBound, double[] targetValues);

	OptimizerInterface getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound, double[] upperBound, double[] parameterStep, double[] targetValues);

}
