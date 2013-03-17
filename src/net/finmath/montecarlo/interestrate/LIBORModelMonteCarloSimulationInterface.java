/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 01.03.2008
 */
package net.finmath.montecarlo.interestrate;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.MonteCarloSimulationInterface;
import net.finmath.montecarlo.interestrate.modelplugins.AbstractLIBORCovarianceModel;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Basic interface which has to be implemented by Monte Carlo models for LIBOR processes.
 * 
 * @author Christian Fries
 * @version 1.0
 */
public interface LIBORModelMonteCarloSimulationInterface extends MonteCarloSimulationInterface {

	/**
	 * @return Returns the numberOfFactors.
	 */
	public abstract int getNumberOfFactors();

	/**
	 * Returns the libor period discretization as time discretization representing start and end dates of periods.
	 * @return Returns the libor period discretization
	 */
	public abstract TimeDiscretizationInterface getLiborPeriodDiscretization();

	/**
	 * @return The number of LIBORs in the LIBOR discretization
	 */
	public abstract int getNumberOfLibors();

	/**
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
	public abstract double getLiborPeriod(int timeIndex);

	/**
	 * Same as java.util.Arrays.binarySearch(liborPeriodDiscretization,time).
	 * Will return a negative value if the time is not found, but then -index-1 corresponds to the index of the smallest time greater than the given one.
	 * 
	 * @return The index corresponding to a given time (interpretation is start of period)
	 */
	public abstract int getLiborPeriodIndex(double time);

	/**
	 * @return the covarianceModel
	 */
	public abstract AbstractLIBORCovarianceModel getCovarianceModel();

	public abstract RandomVariableInterface getLIBOR(int timeIndex, int liborIndex) throws CalculationException;

	/**
	 * @param time          Simulation time
	 * @param periodStart   Start time of period
	 * @param periodEnd     End time of period
	 * @return              The LIBOR rate as seen on simulation time for the specified period
	 * @throws CalculationException 
	 */
	public abstract RandomVariableInterface getLIBOR(double time, double periodStart, double periodEnd) throws CalculationException;

	public abstract RandomVariableInterface[] getLIBORs(int timeIndex) throws CalculationException;

	/**
	 * @param time Time at which the process should be observed
	 * @return The numeraire at the specified time as <code>RandomVariable</code>
	 * @throws CalculationException 
	 */
	public abstract RandomVariableInterface getNumeraire(double time) throws CalculationException;

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 * 
	 * @param timeIndex Time index at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 * @throws CalculationException 
	 */
	public abstract RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException;

	/**
	 * This method returns the weights of a weighted Monte Carlo method (the probability density).
	 * 
	 * @param time Time at which the process should be observed
	 * @return A vector of positive weights which sums up to one
	 * @throws CalculationException 
	 */
	public abstract RandomVariableInterface getMonteCarloWeights(double time) throws CalculationException;
	
	public abstract BrownianMotionInterface getBrownianMotion();

	/**
	 * @return The underlying model
	 */
	public abstract Object getModel();

	/**
	 * @return The implementation of the process
	 */
	public abstract Object getProcess();
	
	public abstract Object getCloneWithModifiedSeed(int seed);
}