/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretizationFromArray;
import net.finmath.time.TimeDiscretization;

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
public class LocalRiskMinimizingHedgePortfolio extends AbstractAssetMonteCarloProduct {

	// Model assumptions for the hedge
	private final AbstractAssetMonteCarloProduct productToHedge;
	private final AssetModelMonteCarloSimulationModel modelUsedForHedging;

	private final TimeDiscretization timeDiscretizationForRebalancing;

	private final int numberOfBins;

	/**
	 * Construction of a variance minimizing hedge portfolio.
	 *
	 * @param productToHedge The financial product for which the hedge portfolio should be constructed.
	 * @param modelUsedForHedging The model used for calculating the hedge rations (deltas). This may differ from the model passed to <code>getValue</code>.
	 * @param timeDiscretizationForRebalancing The times at which the portfolio is re-structured.
	 * @param numberOfBins The number of bins to use in the estimation of the conditional expectation.
	 */
	public LocalRiskMinimizingHedgePortfolio(AbstractAssetMonteCarloProduct productToHedge,
			AssetModelMonteCarloSimulationModel modelUsedForHedging,
			TimeDiscretization timeDiscretizationForRebalancing,
			int numberOfBins) {
		super();
		this.productToHedge = productToHedge;
		this.modelUsedForHedging = modelUsedForHedging;
		this.timeDiscretizationForRebalancing = timeDiscretizationForRebalancing;
		this.numberOfBins = numberOfBins;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) throws CalculationException {

		// Ask the model for its discretization
		int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		int numberOfPath			= model.getNumberOfPaths();

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		/*
		 *  Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		 */
		RandomVariable numeraireToday  = model.getNumeraire(0.0);
		double valueOfOptionAccordingHedgeModel = productToHedge.getValue(modelUsedForHedging);

		// We store the composition of the hedge portfolio (depending on the path)
		RandomVariable amountOfNumeraireAsset		= numeraireToday.invert().mult(valueOfOptionAccordingHedgeModel);
		RandomVariable amountOfUderlyingAsset		= model.getRandomVariableForConstant(0.0);

		for(int timeIndex = 0; timeIndex<timeDiscretizationForRebalancing.getNumberOfTimes()-1; timeIndex++) {
			double time		=	timeDiscretizationForRebalancing.getTime(timeIndex);
			double timeNext	=	timeDiscretizationForRebalancing.getTime(timeIndex+1);

			if(time > evaluationTime) {
				break;
			}

			// Get value of underlying and numeraire assets
			RandomVariable underlyingAtTime = modelUsedForHedging.getAssetValue(time,0);
			RandomVariable numeraireAtTime  = modelUsedForHedging.getNumeraire(time);
			RandomVariable underlyingAtTimeNext = modelUsedForHedging.getAssetValue(timeNext,0);
			RandomVariable numeraireAtTimeNext  = modelUsedForHedging.getNumeraire(timeNext);

			RandomVariable productAtTime		= productToHedge.getValue(time, modelUsedForHedging);
			RandomVariable productAtTimeNext	= productToHedge.getValue(timeNext, modelUsedForHedging);

			RandomVariable[] basisFunctionsEstimator = getBasisFunctions(modelUsedForHedging.getAssetValue(time,0));
			RandomVariable[] basisFunctionsPredictor = getBasisFunctions(model.getAssetValue(time,0));

			MonteCarloConditionalExpectationRegression condExpectationHedging	= new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsEstimator);
			MonteCarloConditionalExpectationRegression condExpectationValuation	= new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsPredictor);

			RandomVariable underlyingRebased = underlyingAtTimeNext.div(numeraireAtTimeNext);
			RandomVariable underlyingRebasedExpected = condExpectationHedging.getConditionalExpectation(underlyingRebased);
			RandomVariable underlyingRebasedMartingale = underlyingRebased.sub(underlyingRebasedExpected);

			RandomVariable derivativeRebased = productAtTimeNext.div(numeraireAtTimeNext);
			RandomVariable derivativeRebasedExpected = condExpectationHedging.getConditionalExpectation(derivativeRebased);
			RandomVariable derivativeRebasedMartingale = derivativeRebased.sub(derivativeRebasedExpected);

			RandomVariable derivativeTimesUnderlying = derivativeRebasedMartingale.mult(underlyingRebasedMartingale);
			RandomVariable derivativeTimesUnderlyingExpected = condExpectationValuation.getConditionalExpectation(derivativeTimesUnderlying);

			RandomVariable underlyingRabasedMartingaleSquared = underlyingRebasedMartingale.squared();
			RandomVariable underlyingRabasedMartingaleSquaredExpected = condExpectationValuation.getConditionalExpectation(underlyingRabasedMartingaleSquared);

			RandomVariable delta = derivativeTimesUnderlyingExpected.div(underlyingRabasedMartingaleSquaredExpected);

			RandomVariable underlyingValue = model.getAssetValue(time,0);
			RandomVariable numeraireValue  = model.getNumeraire(time);

			// Determine the delta hedge
			RandomVariable newNumberOfStocks		= delta;
			RandomVariable stocksToBuy				= newNumberOfStocks.sub(amountOfUderlyingAsset);

			// Ensure self financing
			RandomVariable numeraireAssetsToBuy		= stocksToBuy.mult(underlyingValue).div(numeraireValue).mult(-1);
			RandomVariable newNumberOfNumeraireAsset	= amountOfNumeraireAsset.add(numeraireAssetsToBuy);

			// Update portfolio
			amountOfNumeraireAsset	= newNumberOfNumeraireAsset;
			amountOfUderlyingAsset	= newNumberOfStocks;
		}

		/*
		 * At maturity, calculate the value of the replication portfolio
		 */

		// Get value of underlying and numeraire assets
		RandomVariable underlyingAtEvaluationTime	= model.getAssetValue(evaluationTime,0);
		RandomVariable numeraireAtEvaluationTime	= model.getNumeraire(evaluationTime);

		RandomVariable portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime).add(amountOfUderlyingAsset.mult(underlyingAtEvaluationTime));

		return portfolioValue;
	}

	/**
	 * Create basis functions for a binning.
	 *
	 * @param underlying
	 * @return
	 */
	private RandomVariable[] getBasisFunctions(RandomVariable underlying) {
		double min = underlying.getMin();
		double max = underlying.getMax();

		ArrayList<RandomVariable> basisFunctionList = new ArrayList<>();
		double[] discretization = (new TimeDiscretizationFromArray(min, numberOfBins, (max-min)/numberOfBins)).getAsDoubleArray();
		for(double discretizationStep : discretization) {
			RandomVariable indicator = underlying.sub(discretizationStep).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
			basisFunctionList.add(indicator);
		}

		return basisFunctionList.toArray(new RandomVariable[0]);
	}
}
