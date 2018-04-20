/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiableInterface;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.stochastic.ConditionalExpectationEstimatorInterface;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * This class implements a delta hedged portfolio (a hedge simulator).
 * The delta hedge uses numerical calculation of
 * the delta and - in theory - works for any model implementing <code>AssetModelMonteCarloSimulationInterface</code>
 * and any product implementing <code>AbstractAssetMonteCarloProduct</code>.
 * The results however somewhat depend on the choice of the internal regression basis functions.
 *
 * The <code>getValue</code>-method returns the random variable \( \Pi(t) \) representing the value
 * of the replication portfolio \( \Pi(t) = \phi_0(t) N(t) +  \phi_1(t) S(t) \).
 *
 * @author Christian Fries
 * @version 1.1
 */
public class DeltaHedgedPortfolioWithAAD extends AbstractAssetMonteCarloProduct {

	// The financial product we like to replicate
	private final AbstractAssetMonteCarloProduct productToReplicate;

	private int			orderOfRegressionPolynomial		= 4;

	private double lastOperationTimingValuation = Double.NaN;
	private double lastOperationTimingDerivative = Double.NaN;

	/**
	 * Construction of a delta hedge portfolio. The delta hedge uses numerical calculation of
	 * the delta and - in theory - works for any model implementing <code>AssetModelMonteCarloSimulationInterface</code>
	 * and any product implementing <code>AbstractAssetMonteCarloProduct</code>.
	 * The results however somewhat depend on the choice of the internal regression basis functions.
	 *
	 * @param productToReplicate The product for which the replication portfolio should be build. May be any product implementing the <code>AbstractAssetMonteCarloProduct</code> interface.
	 */
	public DeltaHedgedPortfolioWithAAD(AbstractAssetMonteCarloProduct productToReplicate) {
		super();
		this.productToReplicate = productToReplicate;
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {

		// Ask the model for its discretization
		int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		long timingValuationStart = System.currentTimeMillis();

		RandomVariableDifferentiableInterface value = (RandomVariableDifferentiableInterface) productToReplicate.getValue(model.getTime(0), model);
		RandomVariableInterface exerciseTime = null;
		if(productToReplicate instanceof BermudanOption) {
			exerciseTime = ((BermudanOption) productToReplicate).getLastValuationExerciseTime();
		}

		long timingValuationEnd = System.currentTimeMillis();

		RandomVariableInterface valueOfOption = model.getRandomVariableForConstant(value.getAverage());

		// Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		RandomVariableInterface underlyingToday = model.getAssetValue(0.0,0);
		RandomVariableInterface numeraireToday  = model.getNumeraire(0.0);

		// We store the composition of the hedge portfolio (depending on the path)
		RandomVariableInterface amountOfNumeraireAsset = valueOfOption.div(numeraireToday);
		RandomVariableInterface amountOfUderlyingAsset = model.getRandomVariableForConstant(0.0);

		// Delta of option to replicate
		long timingDerivativeStart = System.currentTimeMillis();
		Map<Long, RandomVariableInterface> gradient = value.getGradient();
		long timingDerivativeEnd = System.currentTimeMillis();

		lastOperationTimingValuation = (timingValuationEnd-timingValuationStart) / 1000.0;
		lastOperationTimingDerivative = (timingDerivativeEnd-timingDerivativeStart) / 1000.0;

		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			// Get value of underlying and numeraire assets
			RandomVariableInterface underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
			RandomVariableInterface numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

			// Get delta
			RandomVariableInterface delta = gradient.get(((RandomVariableDifferentiableInterface)underlyingAtTimeIndex).getID());
			if(delta == null) delta = underlyingAtTimeIndex.mult(0.0);

			delta = delta.mult(numeraireAtTimeIndex);
			
			RandomVariableInterface indicator = new RandomVariable(1.0);
			if(exerciseTime != null) indicator = exerciseTime.barrier(exerciseTime.sub(model.getTime(timeIndex)+0.001), new RandomVariable(1.0), 0.0);

			// Create a conditional expectation estimator with some basis functions (predictor variables) for conditional expectation estimation.
			ArrayList<RandomVariableInterface> basisFunctions = getRegressionBasisFunctionsBinning(underlyingAtTimeIndex, indicator);
			ConditionalExpectationEstimatorInterface conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(basisFunctions.toArray(new RandomVariableInterface[0]));

			delta = delta.getConditionalExpectation(conditionalExpectationOperator);

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

	public double getLastOperationTimingValuation() {
		return lastOperationTimingValuation;
	}

	public double getLastOperationTimingDerivative() {
		return lastOperationTimingDerivative;
	}

	private ArrayList<RandomVariableInterface> getRegressionBasisFunctions(RandomVariableInterface underlying, RandomVariableInterface indicator) {
		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<RandomVariableInterface>();

		// Create basis functions - here: 1, S, S^2, S^3, S^4
		for(int powerOfRegressionMonomial=0; powerOfRegressionMonomial<=orderOfRegressionPolynomial; powerOfRegressionMonomial++) {
			basisFunctions.add(underlying.pow(powerOfRegressionMonomial).mult(indicator));
		}

		return basisFunctions;
	}

	private ArrayList<RandomVariableInterface> getRegressionBasisFunctionsBinning(RandomVariableInterface underlying, RandomVariableInterface indicator) {
		ArrayList<RandomVariableInterface> basisFunctions = new ArrayList<RandomVariableInterface>();

		int numberOfBins = 20;
		double[] values = underlying.getRealizations();
		Arrays.sort(values);
		for(int i = 0; i<numberOfBins; i++) {
			double binLeft = values[(int)((double)i/(double)numberOfBins*values.length)];
			RandomVariableInterface basisFunction = underlying.barrier(underlying.sub(binLeft), new RandomVariable(1.0), 0.0).mult(indicator);
			basisFunctions.add(basisFunction);
		}

		return basisFunctions;
	}
}
