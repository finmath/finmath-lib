/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 20.01.2004
 */
package net.finmath.montecarlo.assetderivativevaluation;

import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotion;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.ProcessEulerScheme;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.stochastic.RandomVariableMutableClone;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class glues together a <code>BlackScholeModel</code> and a Monte-Carlo implementation of a <code>AbstractProcess</code>
 * and forms a Monte-Carlo implementation of the Black-Scholes Model by implementing <code>AssetModelMonteCarloSimulationInterface</code>.
 * 
 * @author Christian Fries
 */
public class MonteCarloBlackScholesModel extends AbstractModel implements AssetModelMonteCarloSimulationInterface {

	private double initialValue;
	private double riskFreeRate;		// Actually the same as the drift (which is not stochastic)
	private double volatility;
	
	private int seed = 3141;

	private ImmutableRandomVariableInterface[]	initialValueVector	= new ImmutableRandomVariableInterface[1];
	private ImmutableRandomVariableInterface	drift;
	private ImmutableRandomVariableInterface	volatilityOnPaths;

	/**
	 * Create a Monte-Carlo simulation using given time discretization.
	 * 
	 * @param timeDiscretization The time discretization
	 * @param numberOfPaths The number of Monte-Carlo path to be used
	 * @param initialValue Spot value
	 * @param riskFreeRate The risk free rate
	 * @param volatility The log volatility
	 */
	public MonteCarloBlackScholesModel(
			TimeDiscretizationInterface timeDiscretization,
			int numberOfPaths,
			double initialValue,
			double riskFreeRate,
			double volatility) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;

		/*
		 * The interface definition requires that we provide the drift and the volatility in terms of random variables.
		 * We construct the corresponding random variables here and will return (immutable) references to them.
		 */
		this.initialValueVector[0]	= new RandomVariableMutableClone(new RandomVariable(0.0, Math.log(initialValue)));
		this.drift					= new RandomVariable(0.0, riskFreeRate - 0.5 * volatility*volatility);
		this.volatilityOnPaths		= new RandomVariable(0.0, volatility);

		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(timeDiscretization, 1 /* numberOfFactors */, numberOfPaths, seed));

		// Link model and process for delegation
		process.setModel(this);
		this.setProcess(process);
	}

	/**
	 * @param initialValue
	 * @param riskFreeRate
	 * @param volatility
	 * @param process
	 */
	public MonteCarloBlackScholesModel(
			double initialValue,
			double riskFreeRate,
			double volatility,
			AbstractProcess process) {
		super();

		this.initialValue	= initialValue;
		this.riskFreeRate	= riskFreeRate;
		this.volatility		= volatility;

		/*
		 * The interface definition requires that we provide the drift and the volatility in terms of random variables.
		 * We construct the corresponding random variables here and will return (immutable) references to them.
		 */
		this.initialValueVector[0]	= new RandomVariableMutableClone(new RandomVariable(0.0, Math.log(initialValue)));
		this.drift					= new RandomVariable(0.0, riskFreeRate - 0.5 * volatility*volatility);
		this.volatilityOnPaths		= new RandomVariable(0.0, volatility);
		
		// Link model and process for delegation
		process.setModel(this);
		this.setProcess(process);
	}

	/**
	 * @return Returns the initialValue.
	 */
	public ImmutableRandomVariableInterface[] getInitialState() {
		return initialValueVector;
	}

	public RandomVariableInterface getDrift(int timeIndex, int componentIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor) {
		return new RandomVariableMutableClone(drift);
	}

	public RandomVariableInterface getFactorLoading(int timeIndex, int factor, int component, ImmutableRandomVariableInterface[] realizationAtTimeIndex) {
		return new RandomVariableMutableClone(volatilityOnPaths);
	}

	@Override
	public void applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
		randomVariable.exp();
	}

	public RandomVariableInterface getAssetValue(double time, int assetIndex) throws CalculationException {
		return getAssetValue(getTimeIndex(time), assetIndex);
	}

	public RandomVariableInterface getAssetValue(int timeIndex, int assetIndex) throws CalculationException {
		return getProcessValue(timeIndex, assetIndex);
	}

	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return getMonteCarloWeights(getTimeIndex(time));
	}

	public RandomVariableInterface getNumeraire(int timeIndex)
	{
		double time = getTime(timeIndex);

		return getNumeraire(time);
	}

	public RandomVariableInterface getNumeraire(double time)
	{
		double numeraireValue = Math.exp(riskFreeRate * time);

		return new RandomVariable(time, numeraireValue);
	}

	@Override
	public RandomVariableInterface getRandomVariableForConstant(double value) {
		return new RandomVariable(0.0, getProcess().getNumberOfPaths(), value);
	}
	
	/* (non-Javadoc)
     * @see net.finmath.montecarlo.model.AbstractModelInterface#getNumberOfComponents()
     */
    public int getNumberOfComponents() {
	    return 1;
    }

    public int getNumberOfAssets() {
		return 1;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return super.toString() + "\n" +
		"MonteCarloBlackScholesModel:\n" +
		"  initial value...:" + initialValue + "\n" +
		"  risk free rate..:" + riskFreeRate + "\n" +
		"  volatiliy.......:" + volatility;
	}

	/**
	 * @return Returns the riskFreeRate.
	 */
	public double getRiskFreeRate() {
		return riskFreeRate;
	}

	/**
	 * @return Returns the volatility.
	 */
	public double getVolatility() {
		return volatility;
	}

	/* (non-Javadoc)
     * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getCloneWithModifiedData(java.util.Map)
     */
    public MonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) {
    	/**
		 *  Default case. Create a new model with the new date
		 */
    	double	newInitialValue	= dataModified.get("initialValue") != null	? ((Number)dataModified.get("initialValue")).doubleValue() : initialValue;
    	double	newRiskFreeRate	= dataModified.get("riskFreeRate") != null	? ((Number)dataModified.get("riskFreeRate")).doubleValue() : riskFreeRate;
    	double	newVolatility	= dataModified.get("volatility") != null	? ((Number)dataModified.get("volatility")).doubleValue()	: volatility;
    	int		newSeed			= dataModified.get("seed") != null			? ((Number)dataModified.get("seed")).intValue()				: seed;

    	if(dataModified.get("seed") != null) {
    		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(this.getTimeDiscretization(), 1, this.getNumberOfPaths(), newSeed));
    		return new MonteCarloBlackScholesModel(newInitialValue, newRiskFreeRate, newVolatility, process);
    	}
    	else
    	{
    		// The seed had not changed. We may reuse the random numbers (brownian motion) of the original model
    		BrownianMotionInterface brownianMotion = this.getProcess().getBrownianMotion();
    		AbstractProcess process = new ProcessEulerScheme(brownianMotion);
    		return new MonteCarloBlackScholesModel(newInitialValue, newRiskFreeRate, newVolatility, process);    		
    	}
    }

    /* (non-Javadoc)
	 * @see net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface#getCloneWithModifiedSeed(int)
	 */
	public AssetModelMonteCarloSimulationInterface getCloneWithModifiedSeed(int seed) {
		// Create a corresponding MC process
		AbstractProcess process = new ProcessEulerScheme(new BrownianMotion(this.getTimeDiscretization(), 1 /* numberOfFactors */, this.getNumberOfPaths(), seed));
		return new MonteCarloBlackScholesModel(initialValue, riskFreeRate, volatility, process);
	}

    /**
     * @return The number of paths.
     * @see net.finmath.montecarlo.process.AbstractProcess#getNumberOfPaths()
     */
	@Override
    public int getNumberOfPaths() {
        return getProcess().getNumberOfPaths();
    }
}
