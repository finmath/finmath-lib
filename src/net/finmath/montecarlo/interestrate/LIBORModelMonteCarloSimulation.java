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

	private LIBORMarketModelInterface model;

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
	public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getMonteCarloWeights(double)
	 */
	public RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException {
		return model.getProcess().getMonteCarloWeights(getTimeIndex(time));
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getNumberOfFactors()
	 */
	public int getNumberOfFactors() {
		return model.getProcess().getNumberOfFactors();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getNumberOfPaths()
	 */
	public int getNumberOfPaths() {
		return model.getProcess().getNumberOfPaths();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTime(int)
	 */
	public double getTime(int timeIndex) {
		return model.getProcess().getTime(timeIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeDiscretization()
	 */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return model.getProcess().getTimeDiscretization();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.MonteCarloSimulationInterface#getTimeIndex(double)
	 */
	public int getTimeIndex(double time) {
		return model.getProcess().getTimeIndex(time);
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getBrownianMotion()
	 */
	public BrownianMotionInterface getBrownianMotion() {
		return model.getProcess().getBrownianMotion();
	}

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLIBOR(int, int)
	 */
	public RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException {
		return model.getLIBOR(timeIndex, liborIndex);
	}

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLIBORs(int)
     */
    public RandomVariableInterface[] getLIBORs(int timeIndex) throws CalculationException
    {
    	RandomVariableInterface[] randomVariableVector = new RandomVariableInterface[getNumberOfComponents()];
    	for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++)	randomVariableVector[componentIndex] = getLIBOR(timeIndex, componentIndex);
 
    	return randomVariableVector;
    }

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLIBOR(double, double, double)
	 */
	public RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException
	{
		int periodStartIndex    = getLiborPeriodIndex(periodStart);
		int periodEndIndex      = getLiborPeriodIndex(periodEnd);

		if(periodStartIndex < 0 || periodEndIndex < 0) throw new CalculationException("LIBOR requested outside libor discretization points. Interpolation not supported yet.");

		int timeIndex           = getTimeIndex(time);

		if(timeIndex < 0) throw new CalculationException("LIBOR requested at time outside simulation discretization points. Interpolation not supported yet.");
		
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

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.interestrate.LIBORModelMonteCarloSimulationInterface#getLiborPeriod(int)
	 */
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
}
