/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.Optimizer.ObjectiveFunction;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface OptimizerFactory {

	Optimizer getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] targetValues);

	Optimizer getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound, double[] upperBound, double[] targetValues);

	Optimizer getOptimizer(ObjectiveFunction objectiveFunction, double[] initialParameters, double[] lowerBound, double[] upperBound, double[] parameterStep, double[] targetValues);

}
