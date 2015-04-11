/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 03.04.2015
 */

package net.finmath.montecarlo.hybridassetinterestrate;

import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface;

/**
 * @author Christian Fries
 */
public interface HybridAssetLIBORModelMonteCarloSimulationInterface extends LIBORModelMonteCarloSimulationInterface, AssetModelMonteCarloSimulationInterface
{
}
