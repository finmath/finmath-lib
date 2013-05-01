/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 29.02.2008
 */
package net.finmath.montecarlo.process;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * @author Christian Fries
 */
public interface AbstractProcessInterface {

    /**
     * This method returns the realization of a component of the process at a certain time index.
     * 
     * @param timeIndex Time index at which the process should be observed
     * @param component Component index of the process
     * @return The process component realizations (given as <code>RandomVariable</code>)
     * @throws CalculationException 
     */
    public abstract RandomVariableInterface getProcessValue(int timeIndex, int component) throws CalculationException;

    /**
     * This method returns the weights of a weighted Monte Carlo method (the probability density).
     * 
     * @param timeIndex Time index at which the process should be observed
     * @return A vector of positive weights which sums up to one
     * @throws CalculationException 
     */
    public abstract RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException;

    /**
     * @return Returns the numberOfComponents.
     */
    public abstract int getNumberOfComponents();

    /**
     * @return Returns the numberOfPaths.
     */
    public abstract int getNumberOfPaths();

    /**
     * @return Returns the numberOfFactors.
     */
    public abstract int getNumberOfFactors();

    /**
     * @return Returns the timeDiscretization.
     */
    public abstract TimeDiscretizationInterface getTimeDiscretization();

    /**
     * @param timeIndex Time index
     * @return Returns the time for a given time index.
     */
    public abstract double getTime(int timeIndex);

    /**
     * @param time
     * @return Returns the time index for a given time
     */
    public abstract int getTimeIndex(double time);

	/**
     * @return Returns the brownian motion used to generate this process
     */
    public abstract BrownianMotionInterface getBrownianMotion();

	/**
	 * Create and return a clone of this process. The clone is not tied to any model, but has the same
	 * process specification, that is, if the model is the same, it would generate the same paths.
	 * 
	 * @return Clone of the process
	 */
	public abstract Object clone();

}