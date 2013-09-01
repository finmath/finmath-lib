/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.Arrays;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a delta hedged portfolio of an European option (a hedge simulator).
 * The hedge is done under the assumption of a Black Scholes Model (even if the pricing model is a different one).
 * 
 * @author Christian Fries
 * @version 1.0
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
	public BlackScholesDeltaHedgedPortfolio(double maturity, double strike, double riskFreeRate, double volatility) {
		super();
		this.maturity = maturity;
		this.strike = strike;
		this.riskFreeRate = riskFreeRate;
		this.volatility = volatility;
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

		/*
		 *  Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		 */
		RandomVariableInterface underlyingToday = model.getAssetValue(0.0,0);
	    RandomVariableInterface numeraireToday  = model.getNumeraire(0.0);
		double initialValueAsset		= underlyingToday.get(0);
		double initialValueNumeraire	= numeraireToday.get(0);

		double valueOfOptionAccordingBlackScholes = 	AnalyticFormulas.blackScholesOptionValue(
				initialValueAsset,
				riskFreeRate,
				volatility,
				maturity,
				strike);
		
		Arrays.fill(amountOfNumeraireAsset, valueOfOptionAccordingBlackScholes/initialValueNumeraire);
		Arrays.fill(amountOfUderlyingAsset, 0.0);
		
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
				 * Change the portfolio according to the trading strategy
				 */

				// Delta of option to replicate
				double delta = AnalyticFormulas.blackScholesOptionDelta(
						underlyingValue,						// current underlying value
						riskFreeRate,
						volatility,
						maturity-model.getTime(timeIndex),	// remaining time
						strike);
				
				// Determine the delta hedge
				double newNumberOfStocks		= delta;
				double stocksToBuy				= newNumberOfStocks				- amountOfUderlyingAsset[path];

				// Ensure self financing
				double numeraireAssetsToBuy			= - (stocksToBuy * underlyingValue) / numeraireValue;
				double newNumberOfNumeraireAsset	= amountOfNumeraireAsset[path] + numeraireAssetsToBuy;

				// Update portfolio
				amountOfNumeraireAsset[path]	= newNumberOfNumeraireAsset;
				amountOfUderlyingAsset[path]	= newNumberOfStocks;
			}
		}

		/*
		 * At maturity, calculate the value of the replication portfolio
		 */
		double[] portfolioValue				= new double[numberOfPath];

		// Get value of underlying and numeraire assets			
		RandomVariableInterface underlyingAtEvaluationTime	= model.getAssetValue(evaluationTime,0);
		RandomVariableInterface numeraireAtEvaluationTime	= model.getNumeraire(evaluationTime);
		for(int path=0; path<underlyingAtEvaluationTime.size(); path++)
		{
			double underlyingValue = underlyingAtEvaluationTime.get(path);

			portfolioValue[path] =
					amountOfNumeraireAsset[path] * numeraireAtEvaluationTime.get(path)
				+	amountOfUderlyingAsset[path] * underlyingValue;
		}

		return new RandomVariable(evaluationTime, portfolioValue);
	}
}
