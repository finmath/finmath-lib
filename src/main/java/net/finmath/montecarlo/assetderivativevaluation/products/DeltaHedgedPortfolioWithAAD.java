/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 27.04.2012
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.automaticdifferentiation.RandomVariableDifferentiable;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.stochastic.ConditionalExpectationEstimator;
import net.finmath.stochastic.RandomVariable;

/**
 * This class implements a delta hedged portfolio (a hedge simulator).
 * The delta hedge uses numerical calculation of
 * the delta and - in theory - works for any model implementing <code>AssetModelMonteCarloSimulationModel</code>
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
	private final AssetMonteCarloProduct productToReplicate;

	private int			orderOfRegressionPolynomial		= 4;

	private double lastOperationTimingValuation = Double.NaN;
	private double lastOperationTimingDerivative = Double.NaN;

	/**
	 * Construction of a delta hedge portfolio. The delta hedge uses numerical calculation of
	 * the delta and - in theory - works for any model implementing <code>AssetModelMonteCarloSimulationModel</code>
	 * and any product implementing <code>AbstractAssetMonteCarloProduct</code>.
	 * The results however somewhat depend on the choice of the internal regression basis functions.
	 *
	 * @param productToReplicate The product for which the replication portfolio should be build. May be any product implementing the <code>AbstractAssetMonteCarloProduct</code> interface.
	 */
	public DeltaHedgedPortfolioWithAAD(AssetMonteCarloProduct productToReplicate) {
		super();
		this.productToReplicate = productToReplicate;
	}

	@Override
	public RandomVariable getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) throws CalculationException {

		// Ask the model for its discretization
		int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		long timingValuationStart = System.currentTimeMillis();

		RandomVariableDifferentiable value = (RandomVariableDifferentiable) productToReplicate.getValue(model.getTime(0), model);
		RandomVariable exerciseTime = null;
		if(productToReplicate instanceof BermudanOption) {
			exerciseTime = ((BermudanOption) productToReplicate).getLastValuationExerciseTime();
		}

		long timingValuationEnd = System.currentTimeMillis();

		RandomVariable valueOfOption = model.getRandomVariableForConstant(value.getAverage());

		// Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		RandomVariable underlyingToday = model.getAssetValue(0.0,0);
		RandomVariable numeraireToday  = model.getNumeraire(0.0);

		// We store the composition of the hedge portfolio (depending on the path)
		RandomVariable amountOfNumeraireAsset = valueOfOption.div(numeraireToday);
		RandomVariable amountOfUderlyingAsset = model.getRandomVariableForConstant(0.0);

		// Delta of option to replicate
		long timingDerivativeStart = System.currentTimeMillis();
		Map<Long, RandomVariable> gradient = value.getGradient();
		long timingDerivativeEnd = System.currentTimeMillis();

		lastOperationTimingValuation = (timingValuationEnd-timingValuationStart) / 1000.0;
		lastOperationTimingDerivative = (timingDerivativeEnd-timingDerivativeStart) / 1000.0;

		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			// Get value of underlying and numeraire assets
			RandomVariable underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
			RandomVariable numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

			// Get delta
			RandomVariable delta = gradient.get(((RandomVariableDifferentiable)underlyingAtTimeIndex).getID());
			if(delta == null) {
				delta = underlyingAtTimeIndex.mult(0.0);
			}

			delta = delta.mult(numeraireAtTimeIndex);

			RandomVariable indicator = new RandomVariableFromDoubleArray(1.0);
			if(exerciseTime != null) {
				indicator = exerciseTime.sub(model.getTime(timeIndex)+0.001).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0));
			}

			// Create a conditional expectation estimator with some basis functions (predictor variables) for conditional expectation estimation.
			ArrayList<RandomVariable> basisFunctions = getRegressionBasisFunctionsBinning(underlyingAtTimeIndex, indicator);
			ConditionalExpectationEstimator conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(basisFunctions.toArray(new RandomVariable[0]));

			delta = delta.getConditionalExpectation(conditionalExpectationOperator);

			/*
			 * Change the portfolio according to the trading strategy
			 */

			// Determine the delta hedge
			RandomVariable newNumberOfStocks	    	= delta;
			RandomVariable stocksToBuy			    	= newNumberOfStocks.sub(amountOfUderlyingAsset);

			// Ensure self financing
			RandomVariable numeraireAssetsToSell   	= stocksToBuy.mult(underlyingAtTimeIndex).div(numeraireAtTimeIndex);
			RandomVariable newNumberOfNumeraireAsset	= amountOfNumeraireAsset.sub(numeraireAssetsToSell);

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

		RandomVariable portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime)
				.add(amountOfUderlyingAsset.mult(underlyingAtEvaluationTime));

		return portfolioValue;
	}

	public double getLastOperationTimingValuation() {
		return lastOperationTimingValuation;
	}

	public double getLastOperationTimingDerivative() {
		return lastOperationTimingDerivative;
	}

	private ArrayList<RandomVariable> getRegressionBasisFunctions(RandomVariable underlying, RandomVariable indicator) {
		ArrayList<RandomVariable> basisFunctions = new ArrayList<RandomVariable>();

		// Create basis functions - here: 1, S, S^2, S^3, S^4
		for(int powerOfRegressionMonomial=0; powerOfRegressionMonomial<=orderOfRegressionPolynomial; powerOfRegressionMonomial++) {
			basisFunctions.add(underlying.pow(powerOfRegressionMonomial).mult(indicator));
		}

		return basisFunctions;
	}

	private ArrayList<RandomVariable> getRegressionBasisFunctionsBinning(RandomVariable underlying, RandomVariable indicator) {
		ArrayList<RandomVariable> basisFunctions = new ArrayList<RandomVariable>();

		if(underlying.isDeterministic()) {
			basisFunctions.add(underlying);
		}
		else {
			int numberOfBins = 20;
			double[] values = underlying.getRealizations();
			Arrays.sort(values);
			for(int i = 0; i<numberOfBins; i++) {
				double binLeft = values[(int)((double)i/(double)numberOfBins*values.length)];
				RandomVariable basisFunction = underlying.sub(binLeft).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0)).mult(indicator);
				basisFunctions.add(basisFunction);
			}
		}

		return basisFunctions;
	}
}
