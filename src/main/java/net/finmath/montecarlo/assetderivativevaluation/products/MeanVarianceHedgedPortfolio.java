/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class implements a mean variance hedged portfolio of a given product (a hedge simulator).
 * The hedge is done using a given model, that is, the model generating the states and the model
 * used to calculate the hedge portfolio may be different!
 * 
 * <br>
 * WARNING: If the model used for calculating the delta is "slow" (e.g., a Monte-Carlo simulation)
 * then the calculation might take very long.
 * <br>
 * 
 * @author Christian Fries
 * @version 1.0
 */
public class MeanVarianceHedgedPortfolio extends AbstractAssetMonteCarloProduct {

	// Model assumptions for the hedge
	private final AbstractAssetMonteCarloProduct productToHedge;
	private final AssetModelMonteCarloSimulationInterface modelUsedForHedging;

	private final TimeDiscretizationInterface timeDiscretizationForRebalancing;
	
	private final int numberOfBins;

	/**
	 * Construction of a variance minimizing hedge portfolio.
	 * 
	 * @param productToHedge The financial product for which the hedge portfolio should be constructed.
	 * @param modelUsedForHedging The model used for calculating the hedge rations (deltas). This may differ from the model passed to <code>getValue</code>.
	 * @param timeDiscretizationForRebalancing The times at which the portfolio is re-structured.
	 * @param numberOfBins The number of bins to use in the estimation of the conditional expectation.
	 */
	public MeanVarianceHedgedPortfolio(AbstractAssetMonteCarloProduct productToHedge,
			AssetModelMonteCarloSimulationInterface modelUsedForHedging,
			TimeDiscretizationInterface timeDiscretizationForRebalancing,
			int numberOfBins) {
		super();
		this.productToHedge = productToHedge;
		this.modelUsedForHedging = modelUsedForHedging;
		this.timeDiscretizationForRebalancing = timeDiscretizationForRebalancing;
		this.numberOfBins = numberOfBins;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {

		// Ask the model for its discretization
		int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		int numberOfPath			= model.getNumberOfPaths();

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		/*
		 *  Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		 */
		RandomVariableInterface numeraireToday  = model.getNumeraire(0.0);
		double valueOfOptionAccordingHedgeModel = productToHedge.getValue(modelUsedForHedging);

		// We store the composition of the hedge portfolio (depending on the path)
		RandomVariableInterface amountOfNumeraireAsset		= numeraireToday.invert().mult(valueOfOptionAccordingHedgeModel);
		RandomVariableInterface amountOfUderlyingAsset		= model.getRandomVariableForConstant(0.0);
		
		for(int timeIndex = 0; timeIndex<timeDiscretizationForRebalancing.getNumberOfTimes()-1; timeIndex++) {
			double time		=	timeDiscretizationForRebalancing.getTime(timeIndex);
			double timeNext	=	timeDiscretizationForRebalancing.getTime(timeIndex+1);

			if(time > evaluationTime) break;

			// Get value of underlying and numeraire assets	
			RandomVariableInterface underlyingAtTime = modelUsedForHedging.getAssetValue(time,0);
			RandomVariableInterface numeraireAtTime  = modelUsedForHedging.getNumeraire(time);
			RandomVariableInterface underlyingAtTimeNext = modelUsedForHedging.getAssetValue(timeNext,0);
			RandomVariableInterface numeraireAtTimeNext  = modelUsedForHedging.getNumeraire(timeNext);

			RandomVariableInterface productAtTime		= productToHedge.getValue(time, modelUsedForHedging);
			RandomVariableInterface productAtTimeNext	= productToHedge.getValue(timeNext, modelUsedForHedging);

			RandomVariableInterface[] basisFunctionsEstimator = getBasisFunctions(modelUsedForHedging.getAssetValue(time,0));
			RandomVariableInterface[] basisFunctionsPredictor = getBasisFunctions(model.getAssetValue(time,0));

			MonteCarloConditionalExpectationRegression condExpectationHedging	= new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsEstimator);
			MonteCarloConditionalExpectationRegression condExpectationValuation	= new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsPredictor);

			RandomVariableInterface S = underlyingAtTimeNext.div(numeraireAtTimeNext);
			RandomVariableInterface ES = condExpectationHedging.getConditionalExpectation(S);
			S = S.sub(ES);

			RandomVariableInterface V = productAtTimeNext.div(numeraireAtTimeNext);
			RandomVariableInterface EV = condExpectationHedging.getConditionalExpectation(V);
			V = V.sub(EV);

			RandomVariableInterface SV = V.mult(S);
			RandomVariableInterface ESV = condExpectationValuation.getConditionalExpectation(SV);

			RandomVariableInterface S2 = S.mult(S);
			RandomVariableInterface ES2 = condExpectationValuation.getConditionalExpectation(S2);

			RandomVariableInterface delta = ESV.div(ES2);

			RandomVariableInterface underlyingValue = model.getAssetValue(time,0);
			RandomVariableInterface numeraireValue  = model.getNumeraire(time);

			// Determine the delta hedge
			RandomVariableInterface newNumberOfStocks		= delta;
			RandomVariableInterface stocksToBuy				= newNumberOfStocks.sub(amountOfUderlyingAsset);

			// Ensure self financing
			RandomVariableInterface numeraireAssetsToBuy		= stocksToBuy.mult(underlyingValue).div(numeraireValue).mult(-1);
			RandomVariableInterface newNumberOfNumeraireAsset	= amountOfNumeraireAsset.add(numeraireAssetsToBuy);

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

		RandomVariableInterface portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime).add(amountOfUderlyingAsset.mult(underlyingAtEvaluationTime));

		return portfolioValue;
	}

	/**
	 * Create basis functions for a binning.
	 * 
	 * @param underlying
	 * @return
	 */
	private RandomVariableInterface[] getBasisFunctions(RandomVariableInterface underlying) {
		double min = underlying.getMin();
		double max = underlying.getMax();

		ArrayList<RandomVariableInterface> basisFunctionList = new ArrayList<RandomVariableInterface>();
		double[] discretization = (new TimeDiscretization(min, numberOfBins, (max-min)/numberOfBins)).getAsDoubleArray();
		for(double discretizationStep : discretization) {
			RandomVariableInterface indicator = underlying.barrier(underlying.sub(discretizationStep), new RandomVariable(1.0), 0.0);
			basisFunctionList.add(indicator);
		}

		return basisFunctionList.toArray(new RandomVariableInterface[0]);
	}
}
