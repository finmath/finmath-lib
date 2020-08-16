package net.finmath.montecarlo.process.component.factortransform;

import net.finmath.stochastic.RandomVariable;

/**
 * @author Christian Fries
 * @version 1.0
 */
public interface FactorTransform {

	/**
	 * The interface describes how an additional factor scaling may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor scaling may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 *
	 * @param timeIndex The time index (associated with the process time discretization).
	 * @param realizationPredictor The realization predictor (in case we use a predictor corrector scheme).
	 * @return The vector of factor scalings.
	 */
	RandomVariable[]	getFactorScaling(int timeIndex, RandomVariable[] realizationPredictor);

	/**
	 * The interface describes how an additional factor drift may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor drift may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 *
	 * @param timeIndex The time index (associated with the process time discretization).
	 * @param realizationPredictor The realization predictor (in case we use a predictor corrector scheme).
	 * @return A vector of random variables given the factor drift for each factor. If the size is less then the number of factors, then higher order factors have no drift.
	 */
	RandomVariable[]	getFactorDrift(int timeIndex, RandomVariable[] realizationPredictor);

	/**
	 * The interface describes how an additional factor drift may be specified for the generation of a process (see e.g. LogNormalProcess).
	 * The factor drift may be used to implement importance sampling or proxy simulation on the level of the discrete process.
	 *
	 * @param timeIndex The time index (associated with the process time discretization).
	 * @param realizationPredictor The realization predictor (in case we use a predictor corrector scheme).
	 * @return The determinant of the factor drift.
	 */
	RandomVariable    getFactorDriftDeterminant(int timeIndex, RandomVariable[] realizationPredictor);
}
