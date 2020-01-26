/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.02.2013
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.AbstractMonteCarloProduct;
import net.finmath.montecarlo.MonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Base class for products requiring an AssetModelMonteCarloSimulationModel for valuation.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractAssetMonteCarloProduct extends AbstractMonteCarloProduct implements AssetMonteCarloProduct {

	/**
	 *
	 */
	public AbstractAssetMonteCarloProduct() {
		super();
	}

	@Override
	public abstract RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) throws CalculationException;

	@Override
	public RandomVariable getValue(final double evaluationTime, final MonteCarloSimulationModel model) throws CalculationException {
		// This product requires an AssetModelMonteCarloSimulationModel model, otherwise there will be a class cast exception
		if(model instanceof AssetModelMonteCarloSimulationModel) {
			return getValue(evaluationTime, (AssetModelMonteCarloSimulationModel)model);
		}
		else {
			throw new IllegalArgumentException("The product " + this.getClass()
			+ " cannot be valued against a model " + model.getClass() + "."
			+ "It requires a model of type " + AssetModelMonteCarloSimulationModel.class + ".");
		}
	}
}
