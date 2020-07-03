/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

/**
 * This class implements a delta hedged portfolio of an European option (a hedge simulator).
 * The hedge is done under the assumption of a Black Scholes Model (even if the pricing model is a different one).
 *
 * The <code>getValue</code>-method returns the random variable \( \Pi(t) \) representing the value
 * of the replication portfolio \( \Pi(t) = \phi_0(t) N(t) +  \phi_1(t) S(t) \).
 *
 * @author Christian Fries
 * @version 1.1
 */
public class BlackScholesDeltaHedgedPortfolio extends AbstractAssetMonteCarloProduct {

	// Properties of the European option we wish to replicate
	private final double maturity;
	private final double strike;

	// Model assumptions for the hedge
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double volatility;

	/**
	 * Construction of a delta hedge portfolio assuming a Black-Scholes model.
	 *
	 * @param maturity		Maturity of the option we wish to replicate.
	 * @param strike		Strike of the option we wish to replicate.
	 * @param riskFreeRate	Model riskFreeRate assumption for our delta hedge.
	 * @param volatility	Model volatility assumption for our delta hedge.
	 */
	public BlackScholesDeltaHedgedPortfolio(final double maturity, final double strike, final double riskFreeRate, final double volatility) {
		super();
		this.maturity = maturity;
		this.strike = strike;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		// Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		final RandomVariable underlyingToday = model.getAssetValue(0.0,0);
		final RandomVariable numeraireToday  = model.getNumeraire(0.0);

		final RandomVariable valueOfOptionAccordingBlackScholes = 	AnalyticFormulas.blackScholesOptionValue(
				underlyingToday,
				riskFreeRate,
				volatility,
				maturity - 0.0,
				strike);

		// We store the composition of the hedge portfolio (depending on the path)
		RandomVariable amountOfNumeraireAsset = valueOfOptionAccordingBlackScholes.div(numeraireToday);
		RandomVariable amountOfUnderlyingAsset = new Scalar(0.0);

		// Ask the model for its discretization
		final int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			// Get value of underlying and numeraire assets
			final RandomVariable underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
			final RandomVariable numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

			// Delta of option to replicate
			final RandomVariable delta = AnalyticFormulas.blackScholesOptionDelta(
					underlyingAtTimeIndex,
					riskFreeRate,
					volatility,
					maturity-model.getTime(timeIndex),	// remaining time
					strike);

			/*
			 * Change the portfolio according to the trading strategy
			 */

			// Determine the delta hedge
			final RandomVariable newNumberOfStocks	= delta;
			final RandomVariable stocksToBuy		= newNumberOfStocks.sub(amountOfUnderlyingAsset);

			// Ensure self financing
			final RandomVariable numeraireAssetsToSell   	= stocksToBuy.mult(underlyingAtTimeIndex).div(numeraireAtTimeIndex);
			final RandomVariable newNumberOfNumeraireAsset	= amountOfNumeraireAsset.sub(numeraireAssetsToSell);

			// Update portfolio
			amountOfNumeraireAsset	= newNumberOfNumeraireAsset;
			amountOfUnderlyingAsset	= newNumberOfStocks;
		}

		/*
		 * At evaluationTime, calculate the value of the replication portfolio
		 */

		// Get value of underlying and numeraire assets
		final RandomVariable underlyingAtEvaluationTime	= model.getAssetValue(evaluationTime,0);
		final RandomVariable numeraireAtEvaluationTime	= model.getNumeraire(evaluationTime);

		final RandomVariable portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime)
				.add(amountOfUnderlyingAsset.mult(underlyingAtEvaluationTime));

		return portfolioValue;
	}
}
