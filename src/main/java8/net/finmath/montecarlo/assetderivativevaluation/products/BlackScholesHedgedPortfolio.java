/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.06.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
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
 * While this is a simple model, it is to some extend resonable, when we assume that the
 * hedge is done by calculating delta from a calibrated model (where the risk free rate and
 * the volatility are "market implied").
 *
 * That said, this class evaluates the hedge portfolio given that the market implies a given
 * risk free rate and volatility, while the underlying follows a given (possibly different) stochastic
 * process.
 *
 * @author Christian Fries
 * @version 1.3
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

		// Ask the model for its discretization
		final int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		final int numberOfPath			= model.getNumberOfPaths();

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		// We store the composition of the hedge portfolio (depending on the path)
		final double[] amountOfUderlyingAsset		= new double[numberOfPath];
		final double[] amountOfNumeraireAsset		= new double[numberOfPath];

		// In case of a gamma hedge, the hedge portfolio consist of additional options
		final double[] amountOfHedgeOptions		= new double[numberOfPath];



		/*
		 *  Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		 */
		final RandomVariable underlyingToday = model.getAssetValue(0.0,0);
		final RandomVariable numeraireToday = model.getNumeraire(0.0);
		final double initialValue = underlyingToday.doubleValue();

		final double valueOfOptionAccordingBlackScholes = 	AnalyticFormulas.blackScholesOptionValue(
				initialValue,
				riskFreeRate,
				volatility,
				maturity,
				strike);

		final double amountOfNumeraireAssetAccordingBlackScholes = valueOfOptionAccordingBlackScholes / numeraireToday.doubleValue();

		Arrays.fill(amountOfNumeraireAsset, amountOfNumeraireAssetAccordingBlackScholes);
		Arrays.fill(amountOfUderlyingAsset, 0.0);
		Arrays.fill(amountOfHedgeOptions, 0.0);

		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			// Get value of underlying and numeraire assets
			final RandomVariable underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
			final RandomVariable numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

			for(int path=0; path<model.getNumberOfPaths(); path++)
			{
				final double underlyingValue	= underlyingAtTimeIndex.get(path);
				final double numeraireValue	= numeraireAtTimeIndex.get(path);

				// Change the portfolio according to the trading strategy

				/*
				 *  Calculate delta and gamma of option to replicate.
				 */

				// Delta of option to replicate
				double delta = AnalyticFormulas.blackScholesOptionDelta(
						underlyingValue,						// current underlying value
						riskFreeRate,
						volatility,
						maturity-model.getTime(timeIndex),	// remaining time
						strike);

				// If we do not perform a gamma hedge, set gamma to zero here, otherwise set it to the gamma of option to replicate.
				double gamma = 0.0;
				if(hedgeOptionStrike != 0) {
					gamma = AnalyticFormulas.blackScholesOptionGamma(
							underlyingValue,						// current underlying value
							riskFreeRate,
							volatility,
							maturity-model.getTime(timeIndex),	// remaining time
							strike);
				}

				// If we do not perform a vega hedge, set vega to zero here, otherwise set it to the gamma of option to replicate.
				double vega = 0.0;
				if(hedgeOptionStrike != 0) {
					vega = AnalyticFormulas.blackScholesOptionVega(
							underlyingValue,						// current underlying value
							riskFreeRate,
							volatility,
							maturity-model.getTime(timeIndex),	// remaining time
							strike) / (maturity-model.getTime(timeIndex));
				}

				/*
				 * If our hedge portfolio consist of a second option (gamma hedge), calculate its price, delta and gamma
				 */

				// Price of option used in hedge
				final double priceOfHedgeOption = AnalyticFormulas.blackScholesOptionValue(
						underlyingValue,						// current underlying value
						riskFreeRate,							// riskFreeRate,
						volatility,								// volatility,										// *(1.0+0.1*(Math.random()-0.5))
						hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
						hedgeOptionStrike);

				// Delta of option used in hedge
				final double deltaOfHedgeOption = AnalyticFormulas.blackScholesOptionDelta(
						underlyingValue,						// current underlying value
						riskFreeRate,							// riskFreeRate,
						volatility,								// volatility,
						hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
						hedgeOptionStrike);

				// Gamma of option used in hedge
				final double gammaOfHedgeOption = AnalyticFormulas.blackScholesOptionGamma(
						underlyingValue,						// current underlying value
						riskFreeRate,							// riskFreeRate,
						volatility,								// volatility,
						hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
						hedgeOptionStrike);

				// Vega of option used in hedge
				final double vegaOfHedgeOption = AnalyticFormulas.blackScholesOptionVega(
						underlyingValue,						// current underlying value
						riskFreeRate,							// riskFreeRate,
						volatility,								// volatility,
						hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
						hedgeOptionStrike) / (hedgeOptionMaturity-model.getTime(timeIndex));


				// Determine the amount of hedge options to buy
				double newNumberOfHedgeOptions	= 0.0;
				switch(hedgeStrategy) {
				case deltaGammaHedge:
					newNumberOfHedgeOptions	= gamma/gammaOfHedgeOption;
					break;
				case deltaVegaHedge:
					newNumberOfHedgeOptions	= vega/vegaOfHedgeOption;
					break;
				case deltaHedge:
				default:
					newNumberOfHedgeOptions	= 0.0;
					break;
				}
				if(Double.isNaN(newNumberOfHedgeOptions) || Double.isInfinite(newNumberOfHedgeOptions) || maturity-model.getTime(timeIndex) < 0.15) {
					newNumberOfHedgeOptions = 0.0;
				}

				final double hedgeOptionsToBuy		= newNumberOfHedgeOptions	- amountOfHedgeOptions[path];
				// Adjust delta
				delta -= newNumberOfHedgeOptions * deltaOfHedgeOption;

				// Determine the delta hedge
				final double newNumberOfStocks		= delta;
				final double stocksToBuy				= newNumberOfStocks				- amountOfUderlyingAsset[path];

				// Ensure self financing
				final double numeraireAssetsToBuy			= - (stocksToBuy * underlyingValue + hedgeOptionsToBuy * priceOfHedgeOption) / numeraireValue;
				final double newNumberOfNumeraireAsset	= amountOfNumeraireAsset[path] + numeraireAssetsToBuy;

				// Update portfolio
				amountOfNumeraireAsset[path]	= newNumberOfNumeraireAsset;
				amountOfUderlyingAsset[path]	= newNumberOfStocks;
				amountOfHedgeOptions[path]		= newNumberOfHedgeOptions;
			}
		}

		/*
		 * At evaluationTime, calculate the value of the replication portfolio
		 */
		//
		final double[] portfolioValue				= new double[numberOfPath];

		// Get value of underlying and numeraire assets
		final RandomVariable underlyingAtEvaluationTime = model.getAssetValue(timeIndexEvaluationTime,0);
		final RandomVariable numeraireAtEvaluationTime  = model.getNumeraire(timeIndexEvaluationTime);
		for(int path=0; path<portfolioValue.length; path++)
		{
			final double underlyingValue = underlyingAtEvaluationTime.get(path);

			// In case we use option to hedge
			double priceOfHedgeOption;
			if(hedgeStrategy.equals(HedgeStrategy.deltaHedge)) {
				priceOfHedgeOption = 0.0;
			}
			else {
				priceOfHedgeOption = AnalyticFormulas.blackScholesOptionValue(
						underlyingValue,						// current underlying value
						riskFreeRate,
						volatility,
						hedgeOptionMaturity-model.getTime(timeIndexEvaluationTime),	// remaining time
						hedgeOptionStrike);
			}

			portfolioValue[path] =
					amountOfNumeraireAsset[path] * numeraireAtEvaluationTime.get(path)
					+	amountOfUderlyingAsset[path] * underlyingValue
					+	amountOfHedgeOptions[path] * priceOfHedgeOption;
		}

		return new RandomVariableFromDoubleArray(evaluationTime, portfolioValue);
	}
}
