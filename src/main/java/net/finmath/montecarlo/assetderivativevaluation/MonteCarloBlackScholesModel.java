/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionLazyInit;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.model.AbstractProcessModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationFromArray;

/**
 * This class glues together a <code>BlackScholeModel</code> and a Monte-Carlo implementation of a <code>MonteCarloProcessFromProcessModel</code>
 * and forms a Monte-Carlo implementation of the Black-Scholes Model by implementing <code>AssetModelMonteCarloSimulationModel</code>.
 *
 * The model is
 * \[
 * 	dS = r S dt + \sigma S dW, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 *
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.MonteCarloProcess}</code> via the specification of
 * \( f = exp \), \( \mu = r - \frac{1}{2} \sigma^2 \), \( \lambda_{1,1} = \sigma \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.MonteCarloProcess} for the notation.
 *
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.MonteCarloProcess The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.ProcessModel The interface for models provinding parameters to numerical schemes.
 * @version 1.0
 */
public class MonteCarloBlackScholesModel implements AssetModelMonteCarloSimulationModel {

	private final BlackScholesModel model;
	private final double initialValue;
	private final int seed = 3141;

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 *
	 * @param timeDiscretizationFromArray The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 */
	public MonteCarloBlackScholesModel(
			TimeDiscretization timeDiscretization,
			int numberOfPaths,
			double initialValue,
			double riskFreeRate,
			double volatility) {
		super();

		this.initialValue = initialValue;

		// Create the model
		model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a corresponding MC process
		MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(new BrownianMotionLazyInit(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);
	}

	/**
	 * Create a Monte-Carlo simulation using given process discretization scheme.
	 *
	 * @param initialValue Spot value
	 * @param riskFreeRate The risk free rate
	 * @param volatility The log volatility
	 * @param process The process discretization scheme which should be used for the simulation.
	 */
	public MonteCarloBlackScholesModel(
			double initialValue,
			double riskFreeRate,
			double volatility,
			MonteCarloProcessFromProcessModel process) {
		super();

		this.initialValue = initialValue;

		// Create the model
		model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getAssetValue(double, int)
	 */
	@Override
	public RandomVariable getAssetValue(double time, int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getAssetValue(int, int)
	 */
	@Override
	public RandomVariable getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return model.getProcess().getProcessValue(timeIndex, assetIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getNumeraire(int)
	 */
	@Override
	public RandomVariable getNumeraire(int timeIndex) throws CalculationException {
		double time = getTime(timeIndex);

		return model.getNumeraire(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getNumeraire(double)
	 */
	@Override
	public RandomVariable getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationModel#getMonteCarloWeights(double)
	 */
	@Override
	public RandomVariable getMonteCarloWeights(double time) throws CalculationException {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getNumberOfAssets()
	 */
	@Override
	public int getNumberOfAssets() {
		return 1;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getCloneWithModifiedData(java.util.Map)
	 * @TODO THE METHOD NEED TO BE CHANGED. NEED
	 */
	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialTime	= dataModified.get("initialTime") != null	? ((Number)dataModified.get("initialTime")).doubleValue() : getTime(0);
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : model.getRiskFreeRate().getAverage();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: model.getVolatility().getAverage();
		int		newSeed			= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		/*
		 * Create a new model with the new model parameters
		 */
		BrownianMotion brownianMotion;
		if(dataModified.get("seed") != null) {
			// The seed has changed. Hence we have to create a new BrownianMotionLazyInit.
			brownianMotion = new BrownianMotionLazyInit(this.getTimeDiscretization(), 1, this.getNumberOfPaths(), newSeed);
		}
		else
		{
			// The seed has not changed. We may reuse the random numbers (Brownian motion) of the original model
			brownianMotion = (BrownianMotion)model.getProcess().getStochasticDriver();
		}

		double timeShift = newInitialTime - getTime(0);
		if(timeShift != 0) {
			ArrayList<Double> newTimes = new ArrayList<>();
			newTimes.add(newInitialTime);
			for(Double time : model.getProcess().getStochasticDriver().getTimeDiscretization()) {
				if(time > newInitialTime) {
					newTimes.add(time);
				}
			}
			TimeDiscretization newTimeDiscretization = new TimeDiscretizationFromArray(newTimes);
			brownianMotion = brownianMotion.getCloneWithModifiedTimeDiscretization(newTimeDiscretization);
		}
		MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(brownianMotion);
		return new MonteCarloBlackScholesModel(newInitialValue, newRiskFreeRate, newVolatility, process);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel#getCloneWithModifiedSeed(int)
	 */
	@Override
	public AssetModelMonteCarloSimulationModel getCloneWithModifiedSeed(int seed) {
		// Create a corresponding MC process
		MonteCarloProcessFromProcessModel process = new EulerSchemeFromProcessModel(new BrownianMotionLazyInit(this.getTimeDiscretization(), 1 /* numberOfFactors */, this.getNumberOfPaths(), seed));
		return new MonteCarloBlackScholesModel(initialValue, model.getRiskFreeRate().getAverage(), model.getVolatility().getAverage(), process);
	}

	@Override
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	@Override
	public LocalDateTime getReferenceDate() {
		return model.getReferenceDate();
	}

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	@Override
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationModel#getRandomVariableForConstant(double)
	 * @TODO Move this to base class
	 */
	@Override
	public RandomVariable getRandomVariableForConstant(double value) {
		return model.getRandomVariableForConstant(value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationModel#getMonteCarloWeights(int)
	 */
	@Override
	public RandomVariable getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	/**
	 * Returns the {@link AbstractProcessModel} used for this Monte-Carlo simulation.
	 *
	 * @return the model
	 */
	public BlackScholesModel getModel() {
		return model;
	}
}
