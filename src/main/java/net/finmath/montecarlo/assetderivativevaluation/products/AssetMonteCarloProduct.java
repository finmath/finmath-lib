/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Interface for products requiring an AssetModelMonteCarloSimulationModel for valuation.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface AssetMonteCarloProduct {

	RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) throws CalculationException;

}
