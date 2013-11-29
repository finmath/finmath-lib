/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 10.06.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a delta and delta-gamma hedged portfolio of an European option (a hedge simulator).
 * 
 * The hedge is done under the assumption of a Black Scholes Model (even if the pricing model is a different one).
 * 
 * @author Christian Fries
 * @version 1.2
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
	 * Construction of a delta hedge portfolio assuming a Black-Scholes model.
	 * 
	 * @param maturity		Maturity of the option we wish to replicate.
	 * @param strike		Strike of the option we wish to replicate.
	 * @param riskFreeRate	Model riskFreeRate assumption for our delta hedge.
	 * @param volatility	Model volatility assumption for our delta hedge.
	 */
	public BlackScholesHedgedPortfolio(double maturity, double strike, double riskFreeRate, double volatility) {
		super();
		this.maturity = maturity;
		this.strike = strike;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;

		// Do not apply gamma hedge
		this.hedgeOptionMaturity	= 0.0;
		this.hedgeOptionStrike	= 0.0;
		this.hedgeStrategy = HedgeStrategy.deltaHedge;
	}

	
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
	public BlackScholesHedgedPortfolio(double maturity, double strike, double riskFreeRate, double volatility,
			double hedgeOptionMaturity, double hedgeOptionStrike, HedgeStrategy hedgeStrategy) {
		super();
		this.maturity				= maturity;
		this.strike					= strike;
		this.riskFreeRate			= riskFreeRate;
		this.volatility				= volatility;
		this.hedgeOptionMaturity	= hedgeOptionMaturity;
		this.hedgeOptionStrike		= hedgeOptionStrike;
		this.hedgeStrategy			= hedgeStrategy;
	}
	
	@Override
	public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {
		
		// Ask the model for its discretization
		int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		int numberOfPath			= model.getNumberOfPaths();

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */
		
		// We store the composition of the hedge portfolio (depending on the path)
		double[] amountOfUderlyingAsset		= new double[numberOfPath];
		double[] amountOfNumeraireAsset		= new double[numberOfPath];
		
		// In case of a gamma hedge, the hedge portfolio consist of additional options
		double[] amountOfHedgeOptions		= new double[numberOfPath];


		
		/*
		 *  Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		 */
		RandomVariableInterface underlyingToday = model.getAssetValue(0.0,0);
		double initialValue = underlyingToday.get(0);

		double valueOfOptionAccordingBlackScholes = 	AnalyticFormulas.blackScholesOptionValue(
				initialValue,
				riskFreeRate,
				volatility,
				maturity,
				strike);
		
		Arrays.fill(amountOfNumeraireAsset,valueOfOptionAccordingBlackScholes);
		Arrays.fill(amountOfUderlyingAsset,0.0);
		Arrays.fill(amountOfHedgeOptions,0.0);
		
		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			// Get value of underlying and numeraire assets			
			RandomVariableInterface underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
		    RandomVariableInterface numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

			for(int path=0; path<model.getNumberOfPaths(); path++)
			{
				double underlyingValue	= underlyingAtTimeIndex.get(path);
				double numeraireValue	= numeraireAtTimeIndex.get(path);

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
				if(hedgeOptionStrike != 0) gamma = AnalyticFormulas.blackScholesOptionGamma(
						underlyingValue,						// current underlying value
						riskFreeRate,
						volatility,
						maturity-model.getTime(timeIndex),	// remaining time
						strike);

				// If we do not perform a vega hedge, set vega to zero here, otherwise set it to the gamma of option to replicate.
				double vega = 0.0;
				if(hedgeOptionStrike != 0) vega = AnalyticFormulas.blackScholesOptionVega(
						underlyingValue,						// current underlying value
						riskFreeRate,
						volatility,
						maturity-model.getTime(timeIndex),	// remaining time
						strike) / (maturity-model.getTime(timeIndex));

				/*
				 * If our hedge portfolio consist of a second option (gamma hedge), calculate its price, delta and gamma
				 */

				// Price of option used in hedge
				double priceOfHedgeOption = AnalyticFormulas.blackScholesOptionValue(
						underlyingValue,						// current underlying value
						riskFreeRate,							// riskFreeRate,
						volatility,								// volatility,										// *(1.0+0.1*(Math.random()-0.5))
						hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
						hedgeOptionStrike);

				// Delta of option used in hedge
				double deltaOfHedgeOption = AnalyticFormulas.blackScholesOptionDelta(
						underlyingValue,						// current underlying value
						riskFreeRate,							// riskFreeRate,
						volatility,								// volatility,
						hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
						hedgeOptionStrike);

				// Gamma of option used in hedge
				double gammaOfHedgeOption = AnalyticFormulas.blackScholesOptionGamma(
						underlyingValue,						// current underlying value
						riskFreeRate,							// riskFreeRate,
						volatility,								// volatility,
						hedgeOptionMaturity-model.getTime(timeIndex),	// remaining time
						hedgeOptionStrike);

				// Vega of option used in hedge
				double vegaOfHedgeOption = AnalyticFormulas.blackScholesOptionVega(
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
				if(Double.isNaN(newNumberOfHedgeOptions) || Double.isInfinite(newNumberOfHedgeOptions) || maturity-model.getTime(timeIndex) < 0.15) newNumberOfHedgeOptions = 0.0;

				double hedgeOptionsToBuy		= newNumberOfHedgeOptions	- amountOfHedgeOptions[path];
				// Adjust delta
				delta -= newNumberOfHedgeOptions * deltaOfHedgeOption;

				// Determine the delta hedge
				double newNumberOfStocks		= delta;
				double stocksToBuy				= newNumberOfStocks				- amountOfUderlyingAsset[path];

				// Ensure self financing
				double numeraireAssetsToBuy			= - (stocksToBuy * underlyingValue + hedgeOptionsToBuy * priceOfHedgeOption) / numeraireValue;
				double newNumberOfNumeraireAsset	= amountOfNumeraireAsset[path] + numeraireAssetsToBuy;

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
		double[] portfolioValue				= new double[numberOfPath];

		// Get value of underlying and numeraire assets			
		RandomVariableInterface underlyingAtEvaluationTime = model.getAssetValue(timeIndexEvaluationTime,0);
		RandomVariableInterface numeraireAtEvaluationTime  = model.getNumeraire(timeIndexEvaluationTime);
		for(int path=0; path<underlyingAtEvaluationTime.size(); path++)
		{
			double underlyingValue = underlyingAtEvaluationTime.get(path);
			
			double priceOfHedgeOption = AnalyticFormulas.blackScholesOptionValue(
					underlyingValue,						// current underlying value
					((MonteCarloBlackScholesModel)model).getRiskFreeRate(),	// riskFreeRate,
					((MonteCarloBlackScholesModel)model).getVolatility(),	// volatility,
					hedgeOptionMaturity-model.getTime(timeIndexEvaluationTime),	// remaining time
					hedgeOptionStrike); 

			portfolioValue[path] =
					amountOfNumeraireAsset[path] * numeraireAtEvaluationTime.get(path)
				+	amountOfUderlyingAsset[path] * underlyingValue
				+	amountOfHedgeOptions[path] * priceOfHedgeOption;
		}
		
		return new RandomVariable(evaluationTime, portfolioValue);
	}
}
