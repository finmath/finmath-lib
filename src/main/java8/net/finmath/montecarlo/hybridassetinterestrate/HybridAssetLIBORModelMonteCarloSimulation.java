/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.04.2015
 */
package net.finmath.montecarlo.hybridassetinterestrate;

import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;

/**
 * Basic interface which has to be implemented by Monte Carlo models for hybrid processes.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface HybridAssetLIBORModelMonteCarloSimulation extends MonteCarloSimulationModel, LIBORModelMonteCarloSimulationModel, AssetModelMonteCarloSimulationModel {
}
