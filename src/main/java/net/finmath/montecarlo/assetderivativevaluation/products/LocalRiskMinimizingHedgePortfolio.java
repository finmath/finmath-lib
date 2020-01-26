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
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

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
	public LocalRiskMinimizingHedgePortfolio(final AbstractAssetMonteCarloProduct productToHedge,
			final AssetModelMonteCarloSimulationModel modelUsedForHedging,
			final TimeDiscretization timeDiscretizationForRebalancing,
			final int numberOfBins) {
		super();
		this.productToHedge = productToHedge;
		this.modelUsedForHedging = modelUsedForHedging;
		this.timeDiscretizationForRebalancing = timeDiscretizationForRebalancing;
		this.numberOfBins = numberOfBins;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {

		// Ask the model for its discretization
		final int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);
		final int numberOfPath			= model.getNumberOfPaths();

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		/*
		 *  Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		 */
		final RandomVariable numeraireToday  = model.getNumeraire(0.0);
		final double valueOfOptionAccordingHedgeModel = productToHedge.getValue(modelUsedForHedging);

		// We store the composition of the hedge portfolio (depending on the path)
		RandomVariable amountOfNumeraireAsset		= numeraireToday.invert().mult(valueOfOptionAccordingHedgeModel);
		RandomVariable amountOfUderlyingAsset		= model.getRandomVariableForConstant(0.0);

		for(int timeIndex = 0; timeIndex<timeDiscretizationForRebalancing.getNumberOfTimes()-1; timeIndex++) {
			final double time		=	timeDiscretizationForRebalancing.getTime(timeIndex);
			final double timeNext	=	timeDiscretizationForRebalancing.getTime(timeIndex+1);

			if(time > evaluationTime) {
				break;
			}

			// Get value of underlying and numeraire assets
			final RandomVariable underlyingAtTime = modelUsedForHedging.getAssetValue(time,0);
			final RandomVariable numeraireAtTime  = modelUsedForHedging.getNumeraire(time);
			final RandomVariable underlyingAtTimeNext = modelUsedForHedging.getAssetValue(timeNext,0);
			final RandomVariable numeraireAtTimeNext  = modelUsedForHedging.getNumeraire(timeNext);

			final RandomVariable productAtTime		= productToHedge.getValue(time, modelUsedForHedging);
			final RandomVariable productAtTimeNext	= productToHedge.getValue(timeNext, modelUsedForHedging);

			final RandomVariable[] basisFunctionsEstimator = getBasisFunctions(modelUsedForHedging.getAssetValue(time,0));
			final RandomVariable[] basisFunctionsPredictor = getBasisFunctions(model.getAssetValue(time,0));

			final MonteCarloConditionalExpectationRegression condExpectationHedging	= new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsEstimator);
			final MonteCarloConditionalExpectationRegression condExpectationValuation	= new MonteCarloConditionalExpectationRegression(basisFunctionsEstimator, basisFunctionsPredictor);

			final RandomVariable underlyingRebased = underlyingAtTimeNext.div(numeraireAtTimeNext);
			final RandomVariable underlyingRebasedExpected = condExpectationHedging.getConditionalExpectation(underlyingRebased);
			final RandomVariable underlyingRebasedMartingale = underlyingRebased.sub(underlyingRebasedExpected);

			final RandomVariable derivativeRebased = productAtTimeNext.div(numeraireAtTimeNext);
			final RandomVariable derivativeRebasedExpected = condExpectationHedging.getConditionalExpectation(derivativeRebased);
			final RandomVariable derivativeRebasedMartingale = derivativeRebased.sub(derivativeRebasedExpected);

			final RandomVariable derivativeTimesUnderlying = derivativeRebasedMartingale.mult(underlyingRebasedMartingale);
			final RandomVariable derivativeTimesUnderlyingExpected = condExpectationValuation.getConditionalExpectation(derivativeTimesUnderlying);

			final RandomVariable underlyingRabasedMartingaleSquared = underlyingRebasedMartingale.squared();
			final RandomVariable underlyingRabasedMartingaleSquaredExpected = condExpectationValuation.getConditionalExpectation(underlyingRabasedMartingaleSquared);

			final RandomVariable delta = derivativeTimesUnderlyingExpected.div(underlyingRabasedMartingaleSquaredExpected);

			final RandomVariable underlyingValue = model.getAssetValue(time,0);
			final RandomVariable numeraireValue  = model.getNumeraire(time);

			// Determine the delta hedge
			final RandomVariable newNumberOfStocks		= delta;
			final RandomVariable stocksToBuy				= newNumberOfStocks.sub(amountOfUderlyingAsset);

			// Ensure self financing
			final RandomVariable numeraireAssetsToBuy		= stocksToBuy.mult(underlyingValue).div(numeraireValue).mult(-1);
			final RandomVariable newNumberOfNumeraireAsset	= amountOfNumeraireAsset.add(numeraireAssetsToBuy);

			// Update portfolio
			amountOfNumeraireAsset	= newNumberOfNumeraireAsset;
			amountOfUderlyingAsset	= newNumberOfStocks;
		}

		/*
		 * At maturity, calculate the value of the replication portfolio
		 */

		// Get value of underlying and numeraire assets
		final RandomVariable underlyingAtEvaluationTime	= model.getAssetValue(evaluationTime,0);
		final RandomVariable numeraireAtEvaluationTime	= model.getNumeraire(evaluationTime);

		final RandomVariable portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime).add(amountOfUderlyingAsset.mult(underlyingAtEvaluationTime));

		return portfolioValue;
	}

	/**
	 * Create basis functions for a binning.
	 *
	 * @param underlying
	 * @return
	 */
	private RandomVariable[] getBasisFunctions(final RandomVariable underlying) {
		final double min = underlying.getMin();
		final double max = underlying.getMax();

		final ArrayList<RandomVariable> basisFunctionList = new ArrayList<>();
		final double[] discretization = (new TimeDiscretizationFromArray(min, numberOfBins, (max-min)/numberOfBins)).getAsDoubleArray();
		for(final double discretizationStep : discretization) {
			final RandomVariable indicator = underlying.sub(discretizationStep).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
			basisFunctionList.add(indicator);
		}

		return basisFunctionList.toArray(new RandomVariable[0]);
	}
}
