package net.finmath.montecarlo.process.component.factordrift;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;

public interface FactorDriftInterface {

	/**
	 * The interface describes how an additional factor scaling may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor scaling may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 *
	 * @param timeIndex The time index (associated with the process time discretization).
	 * @param realizationPredictor The realization predictor (in case we use a predictor corrector scheme).
	 * @return The vector of factor scalings.
	 */
	RandomVariableInterface[]	getFactorScaling(int timeIndex, RandomVariableInterface[] realizationPredictor);

	/**
	 * The interface describes how an additional factor drift may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor drift may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 *
	 * @param timeIndex The time index (associated with the process time discretization).
	 * @param realizationPredictor The realization predictor (in case we use a predictor corrector scheme).
	 * @return A vector of random variables given the factor drift for each factor. If the size is less then the number of factors, then higher order factors have no drift.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariableInterface[]	getFactorDrift(int timeIndex, RandomVariableInterface[] realizationPredictor) throws CalculationException;

	/**
	 * The interface describes how an additional factor drift may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor drift may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 *
	 * @param timeIndex The time index (associated with the process time discretization).
	 * @param realizationPredictor The realization predictor (in case we use a predictor corrector scheme).
	 * @return The determinant of the factor drift.
	 */
	RandomVariableInterface    getFactorDriftDeterminant(int timeIndex, RandomVariableInterface[] realizationPredictor);
}
