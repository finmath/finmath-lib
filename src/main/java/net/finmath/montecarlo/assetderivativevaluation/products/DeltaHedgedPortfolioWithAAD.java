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

	private int			numberOfRegressionFunctions		= 20;

	private double lastOperationTimingValuation = Double.NaN;
	private double lastOperationTimingDerivative = Double.NaN;

	/**
	 * Construction of a delta hedge portfolio. The delta hedge uses numerical calculation of
	 * the delta and - in theory - works for any model implementing <code>AssetModelMonteCarloSimulationModel</code>
	 * and any product implementing <code>AbstractAssetMonteCarloProduct</code>.
	 * The results however somewhat depend on the choice of the internal regression basis functions.
	 *
	 * @param productToReplicate The product for which the replication portfolio should be build. May be any product implementing the <code>AbstractAssetMonteCarloProduct</code> interface.
	 * @param numberOfBins The number of bins used to aggregate the conditional expectation of the delta.
	 */
	public DeltaHedgedPortfolioWithAAD(final AssetMonteCarloProduct productToReplicate, final int numberOfBins) {
		super();
		this.productToReplicate = productToReplicate;
		this.numberOfRegressionFunctions = numberOfBins;
	}

	/**
	 * Construction of a delta hedge portfolio. The delta hedge uses numerical calculation of
	 * the delta and - in theory - works for any model implementing <code>AssetModelMonteCarloSimulationModel</code>
	 * and any product implementing <code>AbstractAssetMonteCarloProduct</code>.
	 * The results however somewhat depend on the choice of the internal regression basis functions.
	 *
	 * @param productToReplicate The product for which the replication portfolio should be build. May be any product implementing the <code>AbstractAssetMonteCarloProduct</code> interface.
	 */
	public DeltaHedgedPortfolioWithAAD(final AssetMonteCarloProduct productToReplicate) {
		super();
		this.productToReplicate = productToReplicate;
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {

		// Ask the model for its discretization
		final int timeIndexEvaluationTime	= model.getTimeIndex(evaluationTime);

		/*
		 *  Going forward in time we monitor the hedge portfolio on each path.
		 */

		final long timingValuationStart = System.currentTimeMillis();

		final RandomVariable value = productToReplicate.getValue(model.getTime(0), model);
		if(!(value instanceof RandomVariableDifferentiable)) {
			throw new IllegalArgumentException("The product's getValue method, when used with the specified model, does not return a RandomVariableDifferentiable. Please check, if the model uses an appropriate RandomVariableFactory.");
		}

		RandomVariable exerciseTime = null;
		if(productToReplicate instanceof BermudanOption) {
			exerciseTime = ((BermudanOption) productToReplicate).getLastValuationExerciseTime();
		}

		final long timingValuationEnd = System.currentTimeMillis();

		final RandomVariable valueOfOption = model.getRandomVariableForConstant(value.getAverage());

		// Initialize the portfolio to zero stocks and as much cash as the Black-Scholes Model predicts we need.
		final RandomVariable underlyingToday = model.getAssetValue(0.0,0);
		final RandomVariable numeraireToday  = model.getNumeraire(0.0);

		// We store the composition of the hedge portfolio (depending on the path)
		RandomVariable amountOfNumeraireAsset = valueOfOption.div(numeraireToday);
		RandomVariable amountOfUderlyingAsset = model.getRandomVariableForConstant(0.0);

		// Delta of option to replicate
		final long timingDerivativeStart = System.currentTimeMillis();
		final Map<Long, RandomVariable> gradient = ((RandomVariableDifferentiable)value).getGradient();
		final long timingDerivativeEnd = System.currentTimeMillis();

		lastOperationTimingValuation = (timingValuationEnd-timingValuationStart) / 1000.0;
		lastOperationTimingDerivative = (timingDerivativeEnd-timingDerivativeStart) / 1000.0;

		for(int timeIndex = 0; timeIndex<timeIndexEvaluationTime; timeIndex++) {
			// Get value of underlying and numeraire assets
			final RandomVariable underlyingAtTimeIndex = model.getAssetValue(timeIndex,0);
			final RandomVariable numeraireAtTimeIndex  = model.getNumeraire(timeIndex);

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
			final ArrayList<RandomVariable> basisFunctions = getRegressionBasisFunctionsBinning(underlyingAtTimeIndex, indicator);
			//			ArrayList<RandomVariable> basisFunctions = getRegressionBasisFunctions(underlyingAtTimeIndex, indicator);

			final ConditionalExpectationEstimator conditionalExpectationOperator = new MonteCarloConditionalExpectationRegression(basisFunctions.toArray(new RandomVariable[0]));

			delta = delta.getConditionalExpectation(conditionalExpectationOperator);

			/*
			 * Change the portfolio according to the trading strategy
			 */

			// Determine the delta hedge
			final RandomVariable newNumberOfStocks	    	= delta;
			final RandomVariable stocksToBuy			    	= newNumberOfStocks.sub(amountOfUderlyingAsset);

			// Ensure self financing
			final RandomVariable numeraireAssetsToSell   	= stocksToBuy.mult(underlyingAtTimeIndex).div(numeraireAtTimeIndex);
			final RandomVariable newNumberOfNumeraireAsset	= amountOfNumeraireAsset.sub(numeraireAssetsToSell);

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

		final RandomVariable portfolioValue = amountOfNumeraireAsset.mult(numeraireAtEvaluationTime)
				.add(amountOfUderlyingAsset.mult(underlyingAtEvaluationTime));

		return portfolioValue;
	}

	public double getLastOperationTimingValuation() {
		return lastOperationTimingValuation;
	}

	public double getLastOperationTimingDerivative() {
		return lastOperationTimingDerivative;
	}

	private ArrayList<RandomVariable> getRegressionBasisFunctions(final RandomVariable underlying, final RandomVariable indicator) {
		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		// Create basis functions - here: 1, S, S^2, S^3, S^4
		for(int powerOfRegressionMonomial=0; powerOfRegressionMonomial<numberOfRegressionFunctions; powerOfRegressionMonomial++) {
			basisFunctions.add(underlying.pow(powerOfRegressionMonomial).mult(indicator));
		}

		return basisFunctions;
	}

	private ArrayList<RandomVariable> getRegressionBasisFunctionsBinning(final RandomVariable underlying, final RandomVariable indicator) {
		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		if(underlying.isDeterministic()) {
			basisFunctions.add(underlying);
		}
		else {
			final double[] values = underlying.getRealizations();
			Arrays.sort(values);
			for(int i = 0; i<numberOfRegressionFunctions; i++) {
				final double binLeft = values[(int)((double)i/(double)numberOfRegressionFunctions*values.length)];
				final RandomVariable basisFunction = underlying.sub(binLeft).choose(new RandomVariableFromDoubleArray(1.0), new RandomVariableFromDoubleArray(0.0)).mult(indicator);
				basisFunctions.add(basisFunction);
			}
		}

		return basisFunctions;
	}
}
