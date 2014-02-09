/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

import java.util.HashMap;
import java.util.Map;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.RandomVariable;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.montecarlo.process.AbstractProcess;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements convenient methods for a libor market model,
 * based on a given <code>LIBORMarketModel</code> model
 * and <code>AbstractLogNormalProcess</code> process.
 * 
 * @author Christian Fries
 * @version 0.6
 */
public class LIBORModelMonteCarloSimulation implements LIBORModelMonteCarloSimulationInterface {

	private final LIBORMarketModelInterface model;

	/**
	 * Create a LIBOR Monte-Carlo Simulation from a given LIBORMarketModel and an AbstractProcess.
	 * 
	 * @param model The LIBORMarketModel.
	 * @param process The process.
	 */
	public LIBORModelMonteCarloSimulation(LIBORMarketModelInterface model, AbstractProcess process) {
		super();
		this.model		= model;
		
		this.model.setProcess(process);
		process.setModel(model);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getMonteCarloWeights(int)
	 */
	@Override
    public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getMonteCarloWeights(double)
	 */
	@Override
    public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(getTimeIndex(time));
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getNumberOfFactors()
	 */
	@Override
    public int getNumberOfFactors() {
		return model.getProcess().getNumberOfFactors();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getNumberOfPaths()
	 */
	@Override
    public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTime(int)
	 */
	@Override
    public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeDiscretization()
	 */
	@Override
    public TimeDiscretizationInterface getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeIndex(double)
	 */
	@Override
    public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

    @Override
    public RandomVariableInterface getRandomVariableForConstant(double value) {
        return model.getProcess().getBrownianMotion().getRandomVariableForConstant(value);
    }

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getBrownianMotion()
     */
	@Override
    public BrownianMotionInterface getBrownianMotion() {
		return model.getProcess().getBrownianMotion();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLIBOR(int, int)
	 */
	@Override
    public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return model.getLIBOR(timeIndex, liborIndex);
	}

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLIBORs(int)
     */
    @Override
    public RandomVariableInterface[] getLIBORs(int timeIndex) throws CalculationException
    {
    	RandomVariableInterface[] randomVariableVector = new RandomVariableInterface[getNumberOfComponents()];
    	for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++)	randomVariableVector[componentIndex] = getLIBOR(timeIndex, componentIndex);
 
    	return randomVariableVector;
    }

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLIBOR(double, double, double)
	 */
	@Override
    public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		int periodStartIndex    = getLiborPeriodIndex(periodStart);
		int periodEndIndex      = getLiborPeriodIndex(periodEnd);

		// The forward rates are provided on fractional tenor discretization points using linear interpolation. See ISBN 0470047224.
		
		// Interpolation on tenor, consistent with interpolation on numeraire: interpolate end date
		if(periodEndIndex < 0) {
			int		previousEndIndex	= -periodEndIndex-1;
			double	previousEndTime		= getLiborPeriod(previousEndIndex);
			double	nextEndTime			= getLiborPeriod(previousEndIndex+1);
			RandomVariableInterface libor				= getLIBOR(time, periodStart, previousEndTime);
			RandomVariableInterface	liborShortPeriod	= getLIBOR(time, previousEndTime, nextEndTime);
			libor = libor.mult(previousEndTime-periodStart).add(1.0).accrue(liborShortPeriod, periodEnd-previousEndTime).sub(1.0).div(periodEnd-periodStart);
			return libor;
		}
		
		// Interpolation on tenor, consistent with interpolation on numeraire: interpolate start date
		if(periodStartIndex < 0) {
			int		previousStartIndex	= -periodStartIndex-1;
			double	previousStartTime	= getLiborPeriod(previousStartIndex);
			double	nextStartTime		= getLiborPeriod(previousStartIndex+1);
			RandomVariableInterface libor				= getLIBOR(time, nextStartTime, periodEnd);
			RandomVariableInterface	liborShortPeriod	= getLIBOR(time, previousStartTime, nextStartTime);
			libor = libor.mult(periodEnd-previousStartTime).add(1.0).discount(liborShortPeriod, periodStart-previousStartTime).sub(1.0).div(periodEnd-periodStart);
			return libor;
		}
		
		if(periodStartIndex < 0 || periodEndIndex < 0) throw new CalculationException("LIBOR requested outside libor discretization points. Interpolation not supported yet.");

		int timeIndex           = getTimeIndex(time);

		if(timeIndex < 0) throw new CalculationException("LIBOR requested at time outside simulation discretization points. Interpolation not supported yet.");
		
		// If this is a model primitive then return it
		if(periodStartIndex+1==periodEndIndex) return getLIBOR(timeIndex, periodStartIndex);

		// The requested LIBOR is not a model primitive. We need to calculate it (slow!)
		RandomVariableInterface accrualAccount = getRandomVariableForConstant(1.0);

		// Calculate the value of the forward bond
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++)
		{
			double subPeriodLength = getLiborPeriod(periodIndex+1) - getLiborPeriod(periodIndex);
			RandomVariableInterface liborOverSubPeriod = getLIBOR(timeIndex, periodIndex);
			
			accrualAccount = accrualAccount.accrue(liborOverSubPeriod, subPeriodLength);
		}

		RandomVariableInterface libor = accrualAccount.sub(1.0).div(periodEnd - periodStart);

		return libor;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLiborPeriod(int)
	 */
	@Override
    public double getLiborPeriod(int timeIndex) {
		return model.getLiborPeriod(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLiborPeriodDiscretization()
	 */
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return model.getLiborPeriodDiscretization();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLiborPeriodIndex(double)
	 */
	public int getLiborPeriodIndex(double time) {
		return model.getLiborPeriodIndex(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getNumberOfComponents()
	 */
	public int getNumberOfComponents() {
		return model.getNumberOfComponents();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getNumberOfLibors()
	 */
	public int getNumberOfLibors() {
		return model.getNumberOfLibors();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getNumeraire(double)
	 */
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getCovarianceModel()
	 */
	public AbstractLIBORCovarianceModel getCovarianceModel() {
		return model.getCovarianceModel();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getModel()
	 */
	public LIBORMarketModelInterface getModel() {
		return model;
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getProcess()
	 */
	public AbstractProcessInterface getProcess() {
		return model.getProcess();
	}

    /* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getCloneWithModifiedSeed(int)
	 */
	public Object getCloneWithModifiedSeed(int seed) {
		AbstractProcess process = (AbstractProcess) ((AbstractProcess)getProcess()).getCloneWithModifiedSeed(seed);
		return new LIBORModelMonteCarloSimulation(model, process);
	}

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getCloneWithModifiedData(java.util.Map)
     */
    public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(Map<String, Object> dataModified) throws CalculationException {
    	LIBORMarketModelInterface modelClone = model.getCloneWithModifiedData(dataModified);
    	return new LIBORModelMonteCarloSimulation(modelClone, (AbstractProcess) getProcess().clone());
    }

	/**
	 * Create a clone of this simulation modifying one of its properties (if any).
	 * 
     * @param entityKey The entity to modify.
	 * @param dataModified The data which should be changed in the new model
	 * @return Returns a clone of this model, where the specified part of the data is modified data (then it is no longer a clone :-)
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
    public LIBORModelMonteCarloSimulationInterface getCloneWithModifiedData(String entityKey, Object dataModified) throws CalculationException
	{
    	Map<String, Object> dataModifiedMap = new HashMap<String, Object>();
    	dataModifiedMap.put(entityKey, dataModified);
    	return getCloneWithModifiedData(dataModifiedMap);
    }
}
