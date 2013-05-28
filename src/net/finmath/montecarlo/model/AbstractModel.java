package net.finmath.montecarlo.model;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.process.AbstractProcessInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class is an abstract base class to implement a model provided to an AbstractProcess
 * 
 * Manages the delegation to AbstractProcessInterface
 * 
 * @author Christian Fries
 * @version 1.3
 */
public abstract class AbstractModel implements AbstractModelInterface {

    private AbstractProcessInterface       process;

    /**
     * Returns the initial value of the model.
     * 
     * @return The initial value of the model.
     */
    public ImmutableRandomVariableInterface[] getInitialValue() {
    	ImmutableRandomVariableInterface[] initialState = getInitialState();
    	
    	RandomVariableInterface[] value = new RandomVariableInterface[initialState.length];
    	for(int i= 0; i<value.length; i++) {
    		value[i] = initialState[i].getMutableCopy();
    		this.applyStateSpaceTransform(i,value[i]);
    	}
    	
    	return value;
    }

    /**
     * @param timeIndex The time index (related to the model times discretization).
     * @param realizationAtTimeIndex The given realization at timeIndex
     * @param realizationPredictor The given realization at <code>timeIndex+1</code> or null if no predictor is available.
     * @return The (average) drift from timeIndex to timeIndex+1
     */
    public RandomVariableInterface[] getDrift(int timeIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor) {

    	RandomVariableInterface[] drift = new RandomVariableInterface[getNumberOfComponents()];
        for(int componentIndex=0; componentIndex<getNumberOfComponents(); componentIndex++) {
        	drift[componentIndex] = getDrift(timeIndex, componentIndex, realizationAtTimeIndex, realizationPredictor);
		}
		return drift;
	}

    /*
     * Delegation to process (numerical scheme)
     */

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.model.AbstractModelInterface#setProcess(net.finmath.montecarlo.process.AbstractProcessInterface)
     */
    public void setProcess(AbstractProcessInterface process) {
        this.process = process;
    }

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.model.AbstractModelInterface#getProcess()
     */
    public AbstractProcessInterface getProcess() {
    	return process;
    }

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.model.AbstractModelInterface#getNumberOfFactors()
     */
    public int getNumberOfFactors() {
        return process.getNumberOfFactors();
    }

    /**
     * @param timeIndex The time index of evaluation time (using this models time discretization)
     * @param componentIndex The component of the process vector
     * @return Process realization as a random variable 
     * @throws CalculationException 
     * @see net.finmath.montecarlo.process.AbstractProcess#getProcessValue(int, int)
     */
    public RandomVariableInterface getProcessValue(int timeIndex, int componentIndex) throws CalculationException {
        return process.getProcessValue(timeIndex, componentIndex);
    }

    /**
     * @param timeIndex The time index of evaluation time (using this models time discretization)
     * @return A random variable representing the Monte-Carlo probabilities.
     * @throws CalculationException 
     * @see net.finmath.montecarlo.process.AbstractProcess#getMonteCarloWeights(int)
     */
    public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException {
        return process.getMonteCarloWeights(timeIndex);
    }

    /**
     * Get the time discretization of the model (simulation time).
     * @return The time discretization of the model (simulation time).
     * @see net.finmath.montecarlo.process.AbstractProcess#getTimeDiscretization()
     */
    public final TimeDiscretizationInterface getTimeDiscretization() {
        return process.getTimeDiscretization();
    }

    /**
     * Return the simulation time for a given time index.
     * @param timeIndex Time index
     * @return Returns the time for a given time index.
     * @see net.finmath.montecarlo.process.AbstractProcess#getTime(int)
     */
    public final double getTime(int timeIndex) {
        return process.getTime(timeIndex);
    }
    

    /**
     * Return the time index associated for the given simulation time.
     * @param time A given time.
     * @return The time index corresponding to the given time.
     * @see net.finmath.montecarlo.process.AbstractProcess#getTimeIndex(double)
     */
    public final int getTimeIndex(double time) {
        return process.getTimeIndex(time);
    }
}
