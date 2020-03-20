/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.08.2018
 */
package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Interfaces for object providing regression basis functions.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface RegressionBasisFunctionsProvider {

	/**
	 * Provides a set of \( \mathcal{F}_{t} \)-measurable random variables which can serve as regression basis functions.
	 *
	 * @param evaluationTime The evaluation time \( t \) at which the basis function should be observed.
	 * @param model The Monte-Carlo model used to derive the basis function.
	 * @return An \( \mathcal{F}_{t} \)-measurable random variable.
	 * @throws CalculationException Thrown if derivation of the basis function fails.
	 */
	RandomVariable[] getBasisFunctions(double evaluationTime, MonteCarloSimulationModel model) throws CalculationException;
}
