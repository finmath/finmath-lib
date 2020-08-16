/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 08.06.2014
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements valuation of a European option on a basket of asset.
 *
 * Given a model for asset \( S_{i} \), the European option with
 * basket weights \( \alpha_{i} \), strike <i>K</i>, maturity <i>T</i>
 * pays \[ max\left( \sum_{i} \alpha_{i} S_{i}(T) - K , 0 \right) \] in <i>T</i>.
 *
 * Note that the specification of \( \alpha_{i} \) and \( K \) allows to construct some
 * special cases, like
 * <ul>
 * 	<li>a European option \( \alpha_{1} = 1 \), \( \alpha_{j} = 0 \) for \( j \neq 1 \) </li>
 * 	<li>an exchange option \( \alpha_{1} = 1 \), \( \alpha_{2} = -1 \), \( K = 0 \) </li>
 * </ul>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class BasketOption extends AbstractAssetMonteCarloProduct {

	private final double	maturity;
	private final double	strike;
	private final double[]	weights;
	private final String[]	nameOfUnderliyngs;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * @param maturity The maturity T in the option payoff \( max\left( \sum_{i} \alpha_{i} S_{i}(T) - K , 0 \right) \).
	 * @param strike The strike K in the option payoff \( max\left( \sum_{i} \alpha_{i} S_{i}(T) - K , 0 \right) \).
	 * @param weights The weights \( \alpha_{i} \) in the option payof \( max\left( \sum_{i} \alpha_{i} S_{i}(T) - K , 0 \right) \).
	 */
	public BasketOption(final double maturity, final double strike, final double[] weights) {
		super();
		this.maturity			= maturity;
		this.strike				= strike;
		this.weights			= weights;
		nameOfUnderliyngs	= null;		// Use asset with index 0, 1, 2, 3
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
	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {
		// Get underlying and numeraire

		RandomVariable values = model.getRandomVariableForConstant(0.0);
		for(int underlyingIndex = 0; underlyingIndex<weights.length; underlyingIndex++) {
			// Get S_{i}(T)
			final RandomVariable underlyingAtMaturity	= model.getAssetValue(maturity, underlyingIndex);
			values = values.addProduct(underlyingAtMaturity, weights[underlyingIndex]);
		}

		// Apply optionality
		values = values.sub(strike).floor(0.0);

		// Discounting...
		final RandomVariable numeraireAtMaturity	= model.getNumeraire(maturity);
		final RandomVariable monteCarloWeights		= model.getMonteCarloWeights(maturity);
		values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		final RandomVariable	numeraireAtEvalTime			= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloWeightsAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
		values = values.mult(numeraireAtEvalTime).div(monteCarloWeightsAtEvalTime);

		return values;
	}
}
