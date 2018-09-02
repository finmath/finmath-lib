/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.08.2018
 */
package net.finmath.montecarlo.conditionalexpectation;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

public interface RegressionBasisFunctionsProvider {
	RandomVariableInterface[] getBasisFunctions(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException;
}
