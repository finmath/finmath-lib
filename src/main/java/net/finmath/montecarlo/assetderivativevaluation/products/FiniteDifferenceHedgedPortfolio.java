/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.06.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements a delta and delta-gamma hedged portfolio of an European option (a hedge simulator).
 *
 * The hedge is done under the assumption of a Black Scholes Model (even if the pricing model is a different one).
 *
 * In case of the gamma hedge and the vega hedge, note that we make the assumption that the
 * market trades these option according to Black-Scholes parameters assumed in hedging.
 * While this is a simple model, it is to some extend reasonable, when we assume that the
 * hedge is done by calculating delta from a calibrated model (where the risk free rate and
 * the volatility are "market implied").
 *
 * That said, this class evaluates the hedge portfolio given that the market implies a given
 * risk free rate and volatility, while the underlying follows a given (possibly different) stochastic
 * process.
 *
 * @author Christian Fries
 * @version 1.3
 * @since finmath-lib 4.1.0
 */
public class FiniteDifferenceHedgedPortfolio extends AbstractAssetMonteCarloProduct {

	public enum HedgeStrategy {
		deltaHedge,
		deltaGammaHedge,
	}

	// Model assumptions for the hedge
	private final AbstractAssetMonteCarloProduct productToHedge;
	private final AssetModelMonteCarloSimulationModel modelUsedForHedging;

	// Hedge portfolio
	private final ArrayList<AbstractAssetMonteCarloProduct> hedgeProducts;

	private final HedgeStrategy hedgeStrategy;

	/**
	 * Construction of a hedge portfolio.
	 *
	 * @param productToHedge The product for which the hedge portfolio should be constructed.
	 * @param modelUsedForHedging The model used for calculation of the hedge ratios.
	 * @param hedgeProducts The products constituting the hedge portfolio.
	 * @param hedgeStrategy	Specification of the hedge strategy to be used (delta, delta-gamma, etc.).
	 */
	public FiniteDifferenceHedgedPortfolio(
			final AbstractAssetMonteCarloProduct productToHedge,
			final AssetModelMonteCarloSimulationModel modelUsedForHedging,
			final ArrayList<AbstractAssetMonteCarloProduct> hedgeProducts,
			final HedgeStrategy hedgeStrategy) {
		super();
		this.productToHedge = productToHedge;
		this.modelUsedForHedging = modelUsedForHedging;
		this.hedgeProducts = hedgeProducts;
		this.hedgeStrategy = hedgeStrategy;
	}


	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {

		// Ask the model for its discretization
		final int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		final int numberOfPath			= model.getNumberOfPaths();

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		// We store the composition of the hedge portfolio (depending on the path)
		final ArrayList<RandomVariable>	amountOfHedgeAssets = new ArrayList<>(hedgeProducts.size());

		/*
		 * Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		 */
		final RandomVariable underlyingToday = model.getAssetValue(0.0,0);
		final double initialValue = underlyingToday.get(0);

		final double valueOfOptionAccordingHedgeModel = productToHedge.getValue(modelUsedForHedging);

		return new RandomVariableFromDoubleArray(evaluationTime, 0.0);
	}
}
