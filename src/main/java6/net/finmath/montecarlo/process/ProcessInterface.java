/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 29.02.2008
 */
package net.finmath.montecarlo.process;

import net.finmath.exception.CalculationException;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * The interface for a stochastic process <i>X</i>.
 * 
 * @author Christian Fries
 */
public interface ProcessInterface {

    /**
     * This method returns the realization of a component of the process at a certain time index.
     * 
     * @param timeIndex Time index at which the process should be observed
     * @param component Component index of the process
     * @return The process component realizations (given as <code>RandomVariable</code>)
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    RandomVariableInterface getProcessValue(int timeIndex, int component) throws CalculationException;

    /**
     * This method returns the weights of a weighted Monte Carlo method (the probability density).
     * 
     * @param timeIndex Time index at which the process should be observed
     * @return A vector of positive weights which sums up to one
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException;

    /**
     * @return Returns the numberOfComponents.
     */
    int getNumberOfComponents();

     /**
     * @return Returns the timeDiscretization.
     */
    TimeDiscretizationInterface getTimeDiscretization();

    /**
     * @param timeIndex Time index.
     * @return Returns the time for a given time index.
     */
    double getTime(int timeIndex);

    /**
     * Returns the time index for a given simulation time.
     * @param time The given simulation time.
     * @return Returns the time index for a given time
     */
    int getTimeIndex(double time);

	/**
	 * Create and return a clone of this process. The clone is not tied to any model, but has the same
	 * process specification, that is, if the model is the same, it would generate the same paths.
	 * 
	 * @return Clone of the process
	 */
    ProcessInterface clone();

}