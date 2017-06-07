/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.math.functions.AnalyticFormulas;
import net.finmath.math.stochastic.RandomVariableInterface;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;

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

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

        // Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
        RandomVariableInterface underlyingToday = model.getAssetValue(0.0,0);
        RandomVariableInterface numeraireToday  = model.getNumeraire(0.0);

        RandomVariableInterface valueOfOptionAccordingBlackScholes = 	AnalyticFormulas.blackScholesGeneralizedOptionValue(
                underlyingToday.mult(Math.exp(riskFreeRate * (maturity - 0.0))),
                model.getRandomVariableForConstant(volatility),
                maturity - 0.0,
                strike,
                model.getRandomVariableForConstant(Math.exp(-riskFreeRate * (maturity - 0.0))));

        // We store the composition of the hedge portfolio (depending on the path)
        RandomVariableInterface amountOfNumeraireAsset = valueOfOptionAccordingBlackScholes.div(numeraireToday);
        RandomVariableInterface amountOfUderlyingAsset = model.getRandomVariableForConstant(0.0);

        for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
            // Get value of underlying and numeraire assets
            RandomVariableInterface underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
            RandomVariableInterface numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

            // Delta of option to replicate
            RandomVariableInterface delta = AnalyticFormulas.blackScholesOptionDelta(
                    underlyingAtTimeIndex,
                    model.getRandomVariableForConstant(riskFreeRate),
                    model.getRandomVariableForConstant(volatility),
                    maturity-model.getTime(timeIndex),	// remaining time
                    strike);

			/*
			 * Change the portfolio according to the trading strategy
			 */

            // Determine the delta hedge
            RandomVariableInterface newNumberOfStocks	    	= delta;
            RandomVariableInterface stocksToBuy			    	= newNumberOfStocks.sub(amountOfUderlyingAsset);

            // Ensure self financing
            RandomVariableInterface numeraireAssetsToSell   	= stocksToBuy.mult(underlyingAtTimeIndex).div(numeraireAtTimeIndex);
            RandomVariableInterface newNumberOfNumeraireAsset	= amountOfNumeraireAsset.sub(numeraireAssetsToSell);

            // Update portfolio
            amountOfNumeraireAsset	= newNumberOfNumeraireAsset;
            amountOfUderlyingAsset	= newNumberOfStocks;
        }

        /*
         * At maturity, calculate the value of the replication portfolio
         */

        // Get value of underlying and numeraire assets
        RandomVariableInterface underlyingAtEvaluationTime	= model.getAssetValue(evaluationTime,0);
        RandomVariableInterface numeraireAtEvaluationTime	= model.getNumeraire(evaluationTime);

        RandomVariableInterface portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime)
                .add(amountOfUderlyingAsset.mult(underlyingAtEvaluationTime));

        return portfolioValue;
    }
}
