/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.05.2015
 */

package net.finmath.optimizer;

import net.finmath.optimizer.StochasticOptimizer.ObjectiveFunction;
import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface StochasticOptimizerFactory {

	default StochasticOptimizer getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, final RandomVariable[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, null, null, null, targetValues);
	}

	default StochasticOptimizer getOptimizer(final ObjectiveFunction objectiveFunction, final RandomVariable[] initialParameters, final RandomVariable[] lowerBound, final RandomVariable[] upperBound, final RandomVariable[] targetValues) {
		return getOptimizer(objectiveFunction, initialParameters, lowerBound, upperBound, null, targetValues);
	}

	StochasticOptimizer getOptimizer(ObjectiveFunction objectiveFunction, RandomVariable[] initialParameters, RandomVariable[] lowerBound, RandomVariable[] upperBound, RandomVariable[] parameterStep, RandomVariable[] targetValues);

}
