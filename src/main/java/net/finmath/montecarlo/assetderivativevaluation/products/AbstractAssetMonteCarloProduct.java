/**
 * 
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Base calls for product that need an AbstractLIBORMarketModel as base class
 * 
 * @author Christian Fries
 */
public abstract class AbstractAssetMonteCarloProduct extends AbstractMonteCarloProduct {

	/**
	 * 
	 */
	public AbstractAssetMonteCarloProduct() {
		super();
	}

	public abstract RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException;

	@Override
	public RandomVariableInterface getValue(double evaluationTime, MonteCarloSimulationInterface model) throws CalculationException {
		// This product requires an AssetModelMonteCarloSimulationInterface model, otherwise there will be a class cast exception
		if(model instanceof AssetModelMonteCarloSimulationInterface) {
			return getValue(evaluationTime, (AssetModelMonteCarloSimulationInterface)model);
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + AssetModelMonteCarloSimulationInterface.class + ".");
		}
	}
}
