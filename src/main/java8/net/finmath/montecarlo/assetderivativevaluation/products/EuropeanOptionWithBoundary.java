/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 21.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation.products;

import java.util.ArrayList;

import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.conditionalexpectation.MonteCarloConditionalExpectationRegression;
import net.finmath.montecarlo.process.component.barrier.Barrier;
import net.finmath.stochastic.RandomVariable;

/**
 * Implements pricing of a European stock option.
 *
 * @author Christian Fries
 * @version 1.2
 * @since finmath-lib 4.1.0
 */
public class EuropeanOptionWithBoundary extends AbstractAssetMonteCarloProduct {

	enum BoundaryAdjustmentType {
		LINEAR_ANALYTIC,
		LINEAR_PROPAGATED,
		SIMPLE_SUPERHEDGE,
		SIMPLE_SUBHEDGE,
		LINEAR_REGRESSED
	}

	private final double boundary = 4;
	private final boolean isBoundaryTimeDep = true;

	private final BoundaryAdjustmentType boundaryAdjustmentType = BoundaryAdjustmentType.LINEAR_REGRESSED;

	private final double maturity;
	private final double strike;

	/**
	 * Create an European option.
	 *
	 * @param maturity The maturity of the European option.
	 * @param strike The strike of the European option.
	 */
	public EuropeanOptionWithBoundary(final double maturity, final double strike) {
		super();
		this.maturity	= maturity;
		this.strike		= strike;
	}

	/**
	 * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
	 * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
	 * Cashflows prior evaluationTime are not considered.
	 *
	 * @param evaluationTime The time on which this products value should be observed.
	 * @param model The model used to price the product.
	 * @return The random variable representing the value of the product discounted to evaluation time
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	@Override
	public RandomVariable getValue(final double evaluationTime, final AssetModelMonteCarloSimulationModel model) throws CalculationException {
		// Get underlying and numeraire
		final RandomVariable underlyingAtMaturity	= model.getAssetValue(maturity,0);

		// The payoff
		final RandomVariable values = underlyingAtMaturity.sub(strike).floor(0.0);

		// Discounting...
		final RandomVariable numeraireAtMaturity		= model.getNumeraire(maturity);
		final RandomVariable monteCarloWeights		= model.getMonteCarloWeights(maturity);
		values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
		final RandomVariable	numeraireAtZero					= model.getNumeraire(evaluationTime);
		final RandomVariable	monteCarloProbabilitiesAtZero	= model.getMonteCarloWeights(evaluationTime);
		values.mult(numeraireAtZero).div(monteCarloProbabilitiesAtZero);

		final RandomVariableFromDoubleArray prob = new RandomVariableFromDoubleArray(0.0,1.0);
		prob.mult(monteCarloWeights).div(monteCarloProbabilitiesAtZero);
		prob.sub(1.0);
		prob.mult(-1.0);

		values.add(getBoundaryAdjustment(evaluationTime, maturity, model, values));

		return values;
	}

	public RandomVariable getBoundaryAdjustment(final double fromTime, final double toTime, final AssetModelMonteCarloSimulationModel model, final RandomVariable continuationValues) throws CalculationException {

		final RandomVariableFromDoubleArray values = new RandomVariableFromDoubleArray(0,0);

		final int fromTimeIndex	= model.getTimeIndex(fromTime);
		final double fromTimeNext = model.getTime(fromTimeIndex+1);

		if(fromTimeNext < toTime) {
			final RandomVariable	monteCarloProbabilitiesEnd			= model.getMonteCarloWeights(fromTimeNext);
			final RandomVariable	monteCarloProbabilitiesStart		= model.getMonteCarloWeights(fromTime);
			final RandomVariable	monteCarloProbabilitiesTransition	= monteCarloProbabilitiesEnd.div(monteCarloProbabilitiesStart);

			final double riskFreeRate = ((MonteCarloBlackScholesModel)model).getModel().getRiskFreeRate().doubleValue();
			final RandomVariable remainingBoundaryAdjustment = this.getBoundaryAdjustment(fromTimeNext, toTime, model, continuationValues);
			remainingBoundaryAdjustment.mult(monteCarloProbabilitiesTransition).mult(Math.exp(-riskFreeRate*(fromTimeNext-fromTime)));

			values.add(remainingBoundaryAdjustment);
		}

		final MonteCarloBlackScholesModel modelBlackScholes = (MonteCarloBlackScholesModel)model;
		final double spot = modelBlackScholes.getModel().getInitialState(modelBlackScholes.getProcess())[0].doubleValue();
		final double riskFreeRate = modelBlackScholes.getModel().getRiskFreeRate().doubleValue();
		final double volatility = modelBlackScholes.getModel().getVolatility().doubleValue();

		double boundaryLocal = spot*Math.exp(riskFreeRate*maturity + boundary * 0.25 * Math.sqrt(maturity));
		//    	boundaryLocal = boundary;
		if(isBoundaryTimeDep) {
			boundaryLocal = spot*Math.exp(riskFreeRate*fromTimeNext + boundary * 0.25 * Math.sqrt(fromTimeNext));
			//		double boundaryLocal = 1*Math.exp(riskFreeRate*fromTimeNext - 0.5 * volatility * volatility*fromTimeNext + boundary * Math.sqrt(fromTimeNext));
			//		double boundaryLocal = 1*Math.exp(riskFreeRate*maturity + boundary * Math.sqrt(fromTimeNext));
		}

		// Boundary adjustment for one time step
		final RandomVariable underlying = model.getAssetValue(fromTime,0);
		final double optionMaturity = fromTimeNext-fromTime;
		final double optionStrike = boundaryLocal;

		final double[] boundaryAdjustmentValues = new double[underlying.size()];

		double c = 0;
		double d = 0;

		if(boundaryAdjustmentType == BoundaryAdjustmentType.LINEAR_ANALYTIC) {
			c = AnalyticFormulas.blackScholesOptionValue(boundaryLocal, riskFreeRate, volatility, toTime-fromTimeNext, strike);
			d = AnalyticFormulas.blackScholesOptionDelta(boundaryLocal, riskFreeRate, volatility, toTime-fromTimeNext, strike);
		}
		else if(boundaryAdjustmentType == BoundaryAdjustmentType.LINEAR_PROPAGATED) {
			c = Math.exp(-riskFreeRate * (fromTimeNext-fromTime)) * boundaryLocal;
			d = Math.exp(-riskFreeRate * (toTime-fromTimeNext));
		}
		else if(boundaryAdjustmentType == BoundaryAdjustmentType.LINEAR_REGRESSED) {
			final RandomVariable weight = new RandomVariableFromDoubleArray(1.0);
			final MonteCarloConditionalExpectationRegression condExpEstimator = new MonteCarloConditionalExpectationRegression(getRegressionBasisFunctions(toTime, model, weight));

			// Calculate cond. expectation. Note that no discounting (numeraire division) is required!
			final double[] paremetersRegressed = condExpEstimator.getLinearRegressionParameters(continuationValues.mult(weight));

			c = paremetersRegressed[0] + boundaryLocal *  paremetersRegressed[1];
			d = paremetersRegressed[1];
		}
		else if(boundaryAdjustmentType == BoundaryAdjustmentType.SIMPLE_SUPERHEDGE) {
			c = boundaryLocal;
			d = 1;
		}
		else if(boundaryAdjustmentType == BoundaryAdjustmentType.SIMPLE_SUBHEDGE) {
			c = boundaryLocal-strike;//*Math.exp(-riskFreeRate * (toTime-fromTimeNext));
			d = 1;
		}

		for(int i=0; i<underlying.size(); i++) {
			final double initialStockValue = underlying.get(i);

			final double a = AnalyticFormulas.blackScholesOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike);
			final double b = AnalyticFormulas.blackScholesDigitalOptionValue(initialStockValue, riskFreeRate, volatility, optionMaturity, optionStrike);

			boundaryAdjustmentValues[i] = c * b + d * a;
		}

		final RandomVariableFromDoubleArray boundaryAdjustment = boundaryAdjustmentValues.length == 1 ? new RandomVariableFromDoubleArray(0.0, boundaryAdjustmentValues[0]) : new RandomVariableFromDoubleArray(0.0, boundaryAdjustmentValues);
		values.add(boundaryAdjustment);

		return values;
	}

	/**
	 * Return the regression basis functions suitable for this product.
	 *
	 * @param exerciseDate The exercise date.
	 * @param model The model to use.
	 * @param weight A weight to apply per path (a random variable)
	 * @return Vector of regression basis functions (vector of random variables).
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	private RandomVariable[] getRegressionBasisFunctions(final double exerciseDate, final AssetModelMonteCarloSimulationModel model, final RandomVariable weight) throws CalculationException {

		final ArrayList<RandomVariable> basisFunctions = new ArrayList<>();

		RandomVariable basisFunction;

		// Constant
		basisFunction = new RandomVariableFromDoubleArray(exerciseDate, 1.0);
		basisFunctions.add(basisFunction.mult(weight));

		// Underlying
		basisFunction = model.getAssetValue(exerciseDate, 0);
		basisFunctions.add(basisFunction.mult(weight));

		return basisFunctions.toArray(new RandomVariable[0]);
	}

	/**
	 * @author Christian Fries
	 *
	 */
	public class ConstantBarrier implements Barrier {

		private final AssetModelMonteCarloSimulationModel	scheme;

		public ConstantBarrier(final AssetModelMonteCarloSimulationModel scheme) {
			super();
			this.scheme	= scheme;
		}

		@Override
		public RandomVariableFromDoubleArray[] getBarrierDirection(final int timeIndex, final RandomVariable[] realizationPredictor) {
			if(timeIndex >= scheme.getTimeDiscretization().getNumberOfTimeSteps()+1) {
				return null;
			}

			final RandomVariableFromDoubleArray[] barrierDirection = new RandomVariableFromDoubleArray[1];
			barrierDirection[0] = new RandomVariableFromDoubleArray(0.0, 1.0);

			return barrierDirection;
		}

		@Override
		public RandomVariableFromDoubleArray getBarrierLevel(final int timeIndex, final RandomVariable[] realizationPredictor) throws CalculationException {
			if(timeIndex >= scheme.getTimeDiscretization().getNumberOfTimeSteps()+1) {
				return null;
			}

			final double  simulationTime  = scheme.getTime(timeIndex);
			final double riskFreeRate = ((MonteCarloBlackScholesModel)scheme).getModel().getRiskFreeRate().doubleValue();
			final double volatility = ((MonteCarloBlackScholesModel)scheme).getModel().getVolatility().doubleValue();

			double boundaryLocal = 1*Math.exp(riskFreeRate*maturity + boundary * 0.25 * Math.sqrt(maturity));
			if(isBoundaryTimeDep) {
				boundaryLocal = 1*Math.exp(riskFreeRate*simulationTime + boundary * 0.25 * Math.sqrt(simulationTime));
			}
			//    		double boundaryLocal = 1*Math.exp(riskFreeRate*simulationTime - 0.5 * volatility * volatility*simulationTime + boundary * volatility * Math.sqrt(simulationTime));
			//    		double boundaryLocal = 1*Math.exp(riskFreeRate*maturity + boundary * Math.sqrt(simulationTime));

			final RandomVariableFromDoubleArray barrierLevel = new RandomVariableFromDoubleArray(simulationTime, Math.log(boundaryLocal));
			final RandomVariable underlying = scheme.getAssetValue(timeIndex-1, 0);
			barrierLevel.sub(underlying.log());
			//            barrierLevel.sub((riskFreeRate)*scheme.getTimeDiscretization().getTimeStep(timeIndex-1));
			barrierLevel.sub((riskFreeRate-0.5*volatility*volatility)*scheme.getTimeDiscretization().getTimeStep(timeIndex-1));

			return barrierLevel;
		}

		/* (non-Javadoc)
		 * @see net.finmath.montecarlo.BarrierInterface#isUpperBarrier()
		 */
		@Override
		public boolean isUpperBarrier() {
			return true;
		}

	}
}
