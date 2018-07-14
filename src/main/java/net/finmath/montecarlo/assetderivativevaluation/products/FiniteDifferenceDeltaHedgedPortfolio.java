/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a delta hedged portfolio of a given product (a hedge simulator).
 * The hedge is done using a given model to calculate delta via finite difference.
 * <br>
 * WARNING: If the model used for calculating the delta is "slow" (e.g., a Monte-Carlo simulation)
 * then the calculation might take very long.
 * <br>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class FiniteDifferenceDeltaHedgedPortfolio extends AbstractAssetMonteCarloProduct {

	// Model assumptions for the hedge
	private final AbstractAssetMonteCarloProduct productToHedge;
	private final AssetModelMonteCarloSimulationInterface modelUsedForHedging;

	/**
	 * Construction of a delta hedge portfolio using finite differences on every path and
	 * in every time-step. Note that this requires many revaluations of the product
	 * provided.
	 *
	 * @param productToHedge The financial product for which the hedge portfolio should be constructed.
	 * @param modelUsedForHedging The model used for calculating the hedge rations (deltas). This may differ from the model passed to <code>getValue</code>.
	 */
	public FiniteDifferenceDeltaHedgedPortfolio(AbstractAssetMonteCarloProduct productToHedge, AssetModelMonteCarloSimulationInterface modelUsedForHedging) {
		super();
		this.productToHedge = productToHedge;
		this.modelUsedForHedging = modelUsedForHedging;
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

		double valueOfOptionAccordingHedgeModel = productToHedge.getValue(modelUsedForHedging);

		Arrays.fill(amountOfNumeraireAsset, valueOfOptionAccordingHedgeModel/initialValueNumeraire);
		Arrays.fill(amountOfUderlyingAsset, 0.0);

		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			double time = model.getTime(timeIndex);

			// Get value of underlying and numeraire assets
			RandomVariableInterface underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
			RandomVariableInterface numeraireAtTimeIndex  = model.getNumeraire(timeIndex);
			for(int path=0; path<model.getNumberOfPaths(); path++)
			{
				double underlyingValue	= underlyingAtTimeIndex.get(path);
				double numeraireValue	= numeraireAtTimeIndex.get(path);

				// Delta of option to replicate
				double shift = initialValueAsset * 1E-8;
				Map<String, Object> dataUpShift = new HashMap<>();
				dataUpShift.put("initialValue", new Double(underlyingValue+shift));
				dataUpShift.put("initialTime", new Double(time));
				Map<String, Object> dataDownShift = new HashMap<>();
				dataDownShift.put("initialValue", new Double(underlyingValue-shift));
				dataDownShift.put("initialTime", new Double(time));
				double delta = (
						productToHedge.getValue(time, modelUsedForHedging.getCloneWithModifiedData(dataUpShift)).getAverage()
						-
						productToHedge.getValue(time, modelUsedForHedging.getCloneWithModifiedData(dataDownShift)).getAverage()
						) / (2*shift);

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
