/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 09.02.2004
 */
package net.finmath.montecarlo.interestrate;

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

	private LIBORMarketModel model;

	public LIBORModelMonteCarloSimulation(LIBORMarketModel model, AbstractProcess process) {
		super();
		this.model		= model;
		
		this.model.setProcess(process);
		process.setModel(model);
	}

	public RandomVariableInterface getProcessValue(int timeIndex, int componentIndex) throws CalculationException {
		return model.getProcess().getProcessValue(timeIndex, componentIndex);
	}

	/**
	 * @param timeIndex
	 * @return
	 * @throws CalculationException 
	 * @see net.finmath.montecarlo.process.AbstractLogNormalProcess#getMonteCarloWeights(int)
	 */
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	/**
	 * @param time
	 * @return
	 * @throws CalculationException 
	 * @see net.finmath.montecarlo.process.AbstractLogNormalProcess#getMonteCarloWeights(int)
	 */
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(getTimeIndex(time));
	}
	
	/**
	 * @return
	 * @see net.finmath.montecarlo.process.AbstractLogNormalProcess#getNumberOfFactors()
	 */
	public int getNumberOfFactors() {
		return model.getProcess().getNumberOfFactors();
	}

	/**
	 * @return
	 * @see net.finmath.montecarlo.process.AbstractLogNormalProcess#getNumberOfPaths()
	 */
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	/**
	 * @param timeIndex
	 * @return
	 * @see net.finmath.montecarlo.process.AbstractLogNormalProcess#getTime(int)
	 */
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	/**
	 * @return
	 * @see net.finmath.montecarlo.process.AbstractLogNormalProcess#getTimeDiscretization()
	 */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	/**
	 * @param time
	 * @return
	 * @see net.finmath.montecarlo.process.AbstractLogNormalProcess#getTimeIndex(double)
	 */
	public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

	public BrownianMotionInterface getBrownianMotion() {
		return model.getProcess().getBrownianMotion();
	}

	/**
	 * @param timeIndex
	 * @param liborIndex
	 * @return
	 * @throws CalculationException 
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getLIBOR(int, int)
	 */
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return model.getLIBOR(timeIndex, liborIndex);
	}

	/**
     * This method returns the vector of LIBORs at a certain time index.
     * 
     * @param timeIndex Time index at which the process should be observed
     * @return The process realizations
	 * @throws CalculationException 
     */
    public RandomVariableInterface[] getLIBORs(int timeIndex) throws CalculationException
    {
    	RandomVariableInterface[] randomVariableVector = new RandomVariableInterface[getNumberOfComponents()];
    	for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++)	randomVariableVector[componentIndex] = getLIBOR(timeIndex, componentIndex);
 
    	return randomVariableVector;
    }

	/**
	 * @param time          Simulation time
	 * @param periodStart   Start time of period
	 * @param periodEnd     End time of period
	 * @return              The LIBOR rate as seen on simulation time for the specified period
	 * @throws CalculationException 
	 */
	public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		int periodStartIndex    = getLiborPeriodIndex(periodStart);
		int periodEndIndex      = getLiborPeriodIndex(periodEnd);

		if(periodStartIndex < 0 || periodEndIndex < 0) throw new CalculationException("LIBOR requested outside discretization points. Interpolation not supported yet.");

		int timeIndex           = getTimeIndex(time);

		if(timeIndex < 0) throw new CalculationException("LIBOR requested at time outside discretization points. Interpolation not supported yet.");
		
		// If this is a model primitive then return it
		if(periodStartIndex+1==periodEndIndex) return getLIBOR(timeIndex, periodStartIndex);

		// The requested LIBOR is not a model primitive. We need to calculate it (slow!)
		double[] libor = new double[getNumberOfPaths()];
		java.util.Arrays.fill(libor,1.0);

		// Calculate the value of the forward bond
		for(int periodIndex = periodStartIndex; periodIndex<periodEndIndex; periodIndex++)
		{
			double subPeriodLength = getLiborPeriod(periodIndex+1) - getLiborPeriod(periodIndex);
			RandomVariableInterface liborOverSubPeriod = getLIBOR(timeIndex, periodIndex);

			for(int path = 0; path<getNumberOfPaths(); path++) {
				libor[path] *= (1 + liborOverSubPeriod.get(path) * subPeriodLength);
			}
		}

		// Calculate the forward rate
		for(int path = 0; path<getNumberOfPaths(); path++) {
			libor[path] -= 1;
			libor[path] /= (periodEnd - periodStart);
		}

		return new RandomVariable(time,libor);
	}

	/**
	 * @param timeIndex
	 * @return
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getLiborPeriod(int)
	 */
	public double getLiborPeriod(int timeIndex) {
		return model.getLiborPeriod(timeIndex);
	}

	/**
	 * @return
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getLiborPeriodDiscretization()
	 */
	public TimeDiscretizationInterface getLiborPeriodDiscretization() {
		return model.getLiborPeriodDiscretization();
	}

	/**
	 * @param time
	 * @return
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getLiborPeriodIndex(double)
	 */
	public int getLiborPeriodIndex(double time) {
		return model.getLiborPeriodIndex(time);
	}

	/**
	 * @return
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getNumberOfComponents()
	 */
	public int getNumberOfComponents() {
		return model.getNumberOfComponents();
	}

	/**
	 * @return
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getNumberOfLibors()
	 */
	public int getNumberOfLibors() {
		return model.getNumberOfLibors();
	}

	/**
	 * @param time
	 * @return
	 * @throws CalculationException 
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getNumeraire(double)
	 */
	public RandomVariableInterface getNumeraire(double time) throws CalculationException {
		return model.getNumeraire(time);
	}

	/**
	 * @param timeIndex
	 * @return
	 * @throws CalculationException 
	 * @see net.finmath.montecarlo.interestrate.LIBORMarketModel#getNumeraire(int)
	 */
	public RandomVariableInterface getNumeraire(int timeIndex) throws CalculationException {
		return model.getNumeraire(timeIndex);
	}

	/**
	 * @return the covarianceModel
	 */
	public AbstractLIBORCovarianceModel getCovarianceModel() {
		return model.getCovarianceModel();
	}

	/**
	 * @return the model
	 */
	public LIBORMarketModelInterface getModel() {
		return model;
	}

	/**
	 * @return the process
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
    	LIBORMarketModel modelClone = model.getCloneWithModifiedData(dataModified);
    	return new LIBORModelMonteCarloSimulation(modelClone, (AbstractProcess) getProcess().clone());
    }
}
