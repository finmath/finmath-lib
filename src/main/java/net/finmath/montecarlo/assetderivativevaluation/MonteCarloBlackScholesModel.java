/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.ArrayList;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretization;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class glues together a <code>BlackScholeModel</code> and a Monte-Carlo implementation of a <code>AbstractProcess</code>
 * and forms a Monte-Carlo implementation of the Black-Scholes Model by implementing <code>AssetModelMonteCarloSimulationInterface</code>.
 *
 * The model is
 * \[
 * 	dS = r S dt + \sigma S dW, \quad S(0) = S_{0},
 * \]
 * \[
 * 	dN = r N dt, \quad N(0) = N_{0},
 * \]
 * 
 * The class provides the model of S to an <code>{@link net.finmath.montecarlo.process.AbstractProcessInterface}</code> via the specification of
 * \( f = exp \), \( \mu = r - \frac{1}{2} \sigma^2 \), \( \lambda_{1,1} = \sigma \), i.e.,
 * of the SDE
 * \[
 * 	dX = \mu dt + \lambda_{1,1} dW, \quad X(0) = \log(S_{0}),
 * \]
 * with \( S = f(X) \). See {@link net.finmath.montecarlo.process.AbstractProcessInterface} for the notation.
 * 
 * @author Christian Fries
 * @see net.finmath.montecarlo.process.AbstractProcessInterface The interface for numerical schemes.
 * @see net.finmath.montecarlo.model.AbstractModelInterface The interface for models provinding parameters to numerical schemes.
 */
public class MonteCarloBlackScholesModel implements AssetModelMonteCarloSimulationInterface {
	
	private final BlackScholesModel model;
	private final double initialValue;
	private final int seed = 3141;

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 * 
	 * @param timeDiscretization The time discretization.
	 * @param numberOfPaths The number of Monte-Carlo path to be used.
	 * @param initialValue Spot value.
	 * @param riskFreeRate The risk free rate.
	 * @param volatility The log volatility.
	 */
	public MonteCarloBlackScholesModel(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfPaths,
			double initialValue,
			double riskFreeRate,
			double volatility) {
		super();

		this.initialValue = initialValue;
		
		// Create the model
		model = new BlackScholesModel(initialValue, riskFreeRate, volatility);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

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
			AbstractProcess process) {
		super();

		this.initialValue = initialValue;

		// Create the model
		model = new BlackScholesModel(initialValue, riskFreeRate, volatility);
		
		// Link model and process for delegation
		process.setModel(model);
		model.setProcess(process);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getAssetValue(double, int)
	 */
	@Override
	public RandomVariableInterface getAssetValue(double time, int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getAssetValue(int, int)
	 */
	@Override
	public RandomVariableInterface getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return model.getProcess().getProcessValue(timeIndex, assetIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getNumeraire(int)
	 */
	@Override
	public RandomVariableInterface getNumeraire(int timeIndex) throws CalculationException {
		double time = getTime(timeIndex);

		return model.getNumeraire(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getNumeraire(double)
	 */
	@Override
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getMonteCarloWeights(double)
	 */
	@Override
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getNumberOfAssets()
	 */
	@Override
	public int getNumberOfAssets() {
		return 1;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getCloneWithModifiedData(java.util.Map)
	 */
	@Override
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) {
		/*
		 * Determine the new model parameters from the provided parameter map.
		 */
		double	newInitialTime	= dataModified.get("initialTime") != null	? ((Number)dataModified.get("initialTime")).doubleValue() : getTime(0);
		double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
		double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : model.getRiskFreeRate();
		double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: model.getVolatility();
		int		newSeed			= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

		/*
		 * Create a new model with the new model parameters
		 */
		BrownianMotionInterface brownianMotion;
		if(dataModified.get("seed") != null) {
			// The seed has changed. Hence we have to create a new BrownianMotion.
			brownianMotion = new BrownianMotion(this.getTimeDiscretization(), 1, this.getNumberOfPaths(), newSeed);
		}
		else
		{
			// The seed has not changed. We may reuse the random numbers (Brownian motion) of the original model
			brownianMotion = (BrownianMotionInterface)model.getProcess().getStochasticDriver();
		}

		double timeShift = newInitialTime - getTime(0);
		if(timeShift != 0) {
			ArrayList<Double> newTimes = new ArrayList<Double>();
			newTimes.add(newInitialTime);
			for(Double time : model.getProcess().getStochasticDriver().getTimeDiscretization()) if(time > newInitialTime) newTimes.add(time);
			TimeDiscretizationInterface newTimeDiscretization = new TimeDiscretization(newTimes);
			brownianMotion = brownianMotion.getCloneWithModifiedTimeDiscretization(newTimeDiscretization);
		}
		AbstractProcess process = new ProcessEulerScheme(brownianMotion);
		return new MonteCarloBlackScholesModel(newInitialValue, newRiskFreeRate, newVolatility, process);    		
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getCloneWithModifiedSeed(int)
	 */
	@Override
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedSeed(int seed) {
		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(this.getTimeDiscretization(), 1 /* numberOfFactors */, this.getNumberOfPaths(), seed));
		return new MonteCarloBlackScholesModel(initialValue, model.getRiskFreeRate(), model.getVolatility(), process);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getNumberOfPaths()
	 */
	@Override
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeDiscretization()
	 */
	@Override
	public TimeDiscretizationInterface getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTime(int)
	 */
	@Override
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeIndex(double)
	 */
	@Override
	public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getRandomVariableForConstant(double)
	 */
	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return model.getProcess().getBrownianMotion().getRandomVariableForConstant(value);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getMonteCarloWeights(int)
	 */
	@Override
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	/**
	 * Returns the {@link AbstractModel} used for this Monte-Carlo simulation.
	 * 
	 * @return the model
	 */
	public BlackScholesModel getModel() {
		return model;
	}
}
