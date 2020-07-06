/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.06.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.Scalar;

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
 * The <code>getValue</code>-method returns the random variable \( \Pi(t) \) representing the value
 * of the replication portfolio \( \Pi(t) = \phi_0(t) N(t) +  \phi_1(t) S(t) +  \psi_0(t) C(t) \).
 *
 * @author Christian Fries
 * @version 1.4
 */
public class BlackScholesHedgedPortfolio extends AbstractAssetMonteCarloProduct {

	public enum HedgeStrategy {
		deltaHedge,
		deltaGammaHedge,
		deltaVegaHedge
	}

	// Properties of the European option we wish to replicate
	private final double maturity;
	private final double strike;

	// Model assumptions for the hedge
	private final double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private final double volatility;

	// Properties for the hedge option if we do a gamma hedge
	private final double hedgeOptionMaturity;
	private final double hedgeOptionStrike;

	private final HedgeStrategy hedgeStrategy;

	/**
	 * Construction of a delta-gamma hedge portfolio assuming a Black-Scholes model.
	 *
	 * @param maturity		Maturity of the option we wish to replicate.
	 * @param strike		Strike of the option we wish to replicate.
	 * @param riskFreeRate	Model riskFreeRate assumption for our delta hedge.
	 * @param volatility	Model volatility assumption for our delta hedge.
	 * @param hedgeOptionMaturity	Maturity of the option used in the hedge portfolio (to hedge gamma).
	 * @param hedgeOptionStrike		Strike of the option used in the hedge portfolio (to hedge gamma).
	 * @param hedgeStrategy			Specification of the hedge strategy to be used (delta, delta-gamma, etc.).
	 */
	public BlackScholesHedgedPortfolio(final double maturity, final double strike, final double riskFreeRate, final double volatility,
			final double hedgeOptionMaturity, final double hedgeOptionStrike, final HedgeStrategy hedgeStrategy) {
		super();
		this.maturity				= maturity;
		this.strike					= strike;
		this.riskFreeRate			= riskFreeRate;
		this.volatility				= volatility;
		this.hedgeOptionMaturity	= hedgeOptionMaturity;
		this.hedgeOptionStrike		= hedgeOptionStrike;
		this.hedgeStrategy			= hedgeStrategy;
	}

	/**
	 * Construction of a hedge portfolio assuming a Black-Scholes model for the hedge ratios.
	 *
	 * @param maturity		Maturity of the option we wish to replicate.
	 * @param strike		Strike of the option we wish to replicate.
	 * @param riskFreeRate	Model riskFreeRate assumption for our delta hedge.
	 * @param volatility	Model volatility assumption for our delta hedge.
	 */
	public BlackScholesHedgedPortfolio(final double maturity, final double strike, final double riskFreeRate, final double volatility) {
		this(maturity, strike, riskFreeRate, volatility, 0.0 /* hedgeOptionMaturity */, 0.0 /* hedgeOptionStrike */, HedgeStrategy.deltaHedge /* hedgeStrategy */);
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
		RandomVariable amountOfNumeraireAsset	= valueOfOptionAccordingBlackScholes.div(numeraireToday);
		RandomVariable amountOfUnderlyingAsset	= model.getRandomVariableForConstant(0.0);
		// In case of a gamma hedge, the hedge portfolio consist of additional options
		RandomVariable amountOfHedgeOptions		= model.getRandomVariableForConstant(0.0);

		// Ask the model for its discretization
		final int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			// Get value of underlying and numeraire assets
			final RandomVariable underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
			final RandomVariable numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

			// Delta of option to replicate
			RandomVariable delta = AnalyticFormulas.blackScholesOptionDelta(
					underlyingAtTimeIndex,
					riskFreeRate,
					volatility,
					maturity-model.getTime(timeIndex),	// remaining time
					strike);

			// If we do not perform a gamma hedge, set gamma to zero here, otherwise set it to the gamma of option to replicate.
			RandomVariable gamma = model.getRandomVariableForConstant(0.0);
			if(hedgeOptionStrike != 0) {
				gamma = AnalyticFormulas.blackScholesOptionGamma(
						underlyingAtTimeIndex,				// current underlying value
						riskFreeRate,
						volatility,
						maturity-model.getTime(timeIndex),	// remaining time
						strike);
			}

			// If we do not perform a vega hedge, set vega to zero here, otherwise set it to the vega of option to replicate.
			RandomVariable vega = model.getRandomVariableForConstant(0.0);
			if(hedgeOptionStrike != 0) {
				vega = AnalyticFormulas.blackScholesOptionVega(
						underlyingAtTimeIndex,				// current underlying value
						riskFreeRate,
						volatility,
						maturity-model.getTime(timeIndex),	// remaining time
						strike);
			}

			/*
			 * If our hedge portfolio consist of a second option (gamma hedge), calculate its price, delta and gamma
			 */

			// Price of option used in hedge
			final RandomVariable priceOfHedgeOption = AnalyticFormulas.blackScholesOptionValue(
					underlyingAtTimeIndex,						// current underlying value
					riskFreeRate,
					volatility,
					hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
					hedgeOptionStrike);

			// Delta of option used in hedge
			final RandomVariable deltaOfHedgeOption = AnalyticFormulas.blackScholesOptionDelta(
					underlyingAtTimeIndex,						// current underlying value
					riskFreeRate,
					volatility,
					hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
					hedgeOptionStrike);

			// Gamma of option used in hedge
			final RandomVariable gammaOfHedgeOption = AnalyticFormulas.blackScholesOptionGamma(
					underlyingAtTimeIndex,						// current underlying value
					riskFreeRate,
					volatility,
					hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
					hedgeOptionStrike);

			// Vega of option used in hedge
			final RandomVariable vegaOfHedgeOption = AnalyticFormulas.blackScholesOptionVega(
					underlyingAtTimeIndex,						// current underlying value
					riskFreeRate,
					volatility,
					hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
					hedgeOptionStrike);

			/*
			 * Change the portfolio according to the trading strategy
			 */

			// Determine the amount of hedge options to buy
			RandomVariable newNumberOfHedgeOptions;
			switch(hedgeStrategy) {
			case deltaGammaHedge:
				newNumberOfHedgeOptions	= gamma.div(gammaOfHedgeOption);
				break;
			case deltaVegaHedge:
				newNumberOfHedgeOptions	= vega.div(vegaOfHedgeOption);
				break;
			case deltaHedge:
			default:
				newNumberOfHedgeOptions	= new Scalar(0.0);
				break;
			}

			final RandomVariable hedgeOptionsToBuy		= newNumberOfHedgeOptions.sub(amountOfHedgeOptions);

			// Adjust delta: remainingDelta = delta - newNumberOfHedgeOptions * deltaOfHedgeOption
			delta = delta.sub(newNumberOfHedgeOptions.mult(deltaOfHedgeOption));

			// Determine the delta hedge
			final RandomVariable newNumberOfStocks	= delta;
			final RandomVariable stocksToBuy		= newNumberOfStocks.sub(amountOfUnderlyingAsset);

			// Ensure self financing
			final RandomVariable numeraireAssetsToSell   	= stocksToBuy.mult(underlyingAtTimeIndex).add(hedgeOptionsToBuy.mult(priceOfHedgeOption)).div(numeraireAtTimeIndex);
			final RandomVariable newNumberOfNumeraireAsset	= amountOfNumeraireAsset.sub(numeraireAssetsToSell);

			// Update portfolio
			amountOfNumeraireAsset	= newNumberOfNumeraireAsset;
			amountOfUnderlyingAsset	= newNumberOfStocks;
			amountOfHedgeOptions = newNumberOfHedgeOptions;
		}

		/*
		 * At evaluationTime, calculate the value of the replication portfolio
		 */

		// Get value of underlying and numeraire assets
		final RandomVariable underlyingAtEvaluationTime	= model.getAssetValue(evaluationTime,0);
		final RandomVariable numeraireAtEvaluationTime	= model.getNumeraire(evaluationTime);

		final RandomVariable priceOfHedgeOption;
		if(hedgeStrategy.equals(HedgeStrategy.deltaHedge)) {
			priceOfHedgeOption = new Scalar(0.0);
		}
		else {
			priceOfHedgeOption = AnalyticFormulas.blackScholesOptionValue(
					underlyingAtEvaluationTime,						// current underlying value
					model.getRandomVariableForConstant(riskFreeRate),
					model.getRandomVariableForConstant(volatility),
					hedgeOptionMaturity-model.getTime(timeIndexEvaluationTime),	// remaining time
					hedgeOptionStrike);
		}

		final RandomVariable portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime)
				.add(amountOfUnderlyingAsset.mult(underlyingAtEvaluationTime))
				.add(amountOfHedgeOptions.mult(priceOfHedgeOption));

		return portfolioValue;
	}
}
