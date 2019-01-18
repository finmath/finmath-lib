/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.04.2015
 */

package net.finmath.montecarlo.hybridassetinterestrate;

import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;

/**
 * @author Christian Fries
 * @version 1.0
 */
public interface HybridAssetLIBORModelMonteCarloSimulationInterface extends LIBORModelMonteCarloSimulationModel, AssetModelMonteCarloSimulationModel
{
}
