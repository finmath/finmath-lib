package net.finmath.montecarlo.process.component.factordrift;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;

public interface FactorDriftInterface {

	/**
	 * The interface describes how an additional factor scaling may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor scaling may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 * 
	 * @param timeIndex
	 * @param realizationPredictor
	 * @return
	 */
	RandomVariableInterface[]	getFactorScaling(int timeIndex, RandomVariableInterface[] realizationPredictor);

	/**
	 * The interface describes how an additional factor drift may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor drift may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 * 
	 * @param timeIndex
	 * @param realizationPredictor
	 * @return A vector of random variables given the factor drift for each factor. If the size is less then the number of factors, then higher order factors have no drift.
	 * @throws CalculationException 
	 */
	RandomVariableInterface[]	getFactorDrift(int timeIndex, RandomVariableInterface[] realizationPredictor) throws CalculationException;

    /**
     * The interface describes how an additional factor drift may be specified for the generation of a process (see e.g. LogNormalProcess).
     * The factor drift may be used to implement importance sampling or proxy simulation on the level of the discrete process.
     * 
     * @param timeIndex
     * @param realizationPredictor
     * @return
     */
	RandomVariableInterface    getFactorDriftDeterminant(int timeIndex, RandomVariableInterface[] realizationPredictor);
}
