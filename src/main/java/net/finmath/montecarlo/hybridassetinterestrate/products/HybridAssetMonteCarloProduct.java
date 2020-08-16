/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 19.05.2013
 */

package net.finmath.montecarlo.hybridassetinterestrate.products;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.hybridassetinterestrate.HybridAssetMonteCarloSimulation;
import net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationModel;
import net.finmath.montecarlo.process.component.factortransform.FactorTransform;
import net.finmath.stochastic.RandomVariable;

/**
 * Base class for product that need an HybridAssetLIBORModelMonteCarloSimulationInterface in their valuation.
 *
 * @author Christian Fries
 */
public abstract class HybridAssetMonteCarloProduct extends AbstractMonteCarloProduct {

	/**
	 *
	 */
	public HybridAssetMonteCarloProduct() {
		super();
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public abstract RandomVariable getValue(double evaluationTime, HybridAssetMonteCarloSimulation model) throws CalculationException;

	public RandomVariable getValueForModifiedData(double evaluationTime, MonteCarloSimulationModel monteCarloSimulationInterface, Map<String, Object> dataModified) throws CalculationException
	{
		return getValue(evaluationTime, monteCarloSimulationInterface.getCloneWithModifiedData(dataModified));
	}

	/**
	 * This method returns the valuation of the product within the specified model, evaluated at a given evalutationTime.
	 * The valuation is returned in terms of a map. The map may contain additional information.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	public Map<String, Object> getValues(double evaluationTime, HybridAssetMonteCarloSimulation model) throws CalculationException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, MonteCarloSimulationModel model) throws CalculationException {
		return getValue(evaluationTime, (HybridAssetMonteCarloSimulation)model);
	}

	/**
	 * Overwrite this method if the product supplies a custom FactorDriftInterface to be used in proxy simulation.
	 *
	 * @param referenceScheme The reference scheme
	 * @param targetScheme The target scheme
	 * @return The FactorDriftInterface
	 */
	public FactorTransform getFactorDrift(LIBORModelMonteCarloSimulationModel referenceScheme, LIBORModelMonteCarloSimulationModel targetScheme) {
		return null;
	}

}
