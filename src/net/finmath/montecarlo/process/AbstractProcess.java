/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 19.01.2004
 */
package net.finmath.montecarlo.process;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.BrownianMotionInterface;
import net.finmath.montecarlo.model.AbstractModel;
import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class is an abstract base class to implement a multi-dimensional multi-factor Ito process.
 * The dimension is called <code>numberOfComponents</code> here. The default for <code>numberOfFactors</code> is 1.
 * 
 * Manages the time discretization and delegation to model.
 * 
 * @author Christian Fries
 * @version 1.4
 */
public abstract class AbstractProcess implements AbstractProcessInterface {

    private AbstractModelInterface			model;
    private TimeDiscretizationInterface		timeDiscretization;

	/**
	 * @param timeDiscretization
	 */
	public AbstractProcess(TimeDiscretizationInterface timeDiscretization) {
		super();
		this.timeDiscretization	= timeDiscretization;
	}

	public abstract Object getCloneWithModifiedSeed(int seed);	

	/**
     * This method returns the realization of the process at a certain time index.
     * 
     * @param timeIndex Time index at which the process should be observed
     * @param componentIndex Component of the process vector
     * @return A vector of process realizations (on path)
	 * @throws CalculationException 
     */
    abstract public RandomVariableInterface getProcessValue(int timeIndex, int componentIndex) throws CalculationException;

    /**
     * This method returns the Monte-Carlo weights associated with the process at a certain time index.
     * 
     * @param timeIndex Time index at which the process should be observed.
     * @throws CalculationException 
     */
    abstract public RandomVariableInterface getMonteCarloWeights(int timeIndex) throws CalculationException;

	/* (non-Javadoc)
	 * @see net.finmath.montecarlo.process.AbstractProcessInterface#getBrownianMotion()
	 */
	abstract public BrownianMotionInterface getBrownianMotion();

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	public abstract AbstractProcessInterface clone();	

	
    /*
     * Delegation to model
     */

    public void setModel(AbstractModel model) {
    	if(this.model != null) throw new RuntimeException("Attemp to reuse process with a different model. This process is already associated with a model.");

    	this.model = model;
    }

    public int getNumberOfComponents() {
        return model.getNumberOfComponents();
    }
	
    public ImmutableRandomVariableInterface[]	getInitialValue() {
        return model.getInitialState();
    };

    public ImmutableRandomVariableInterface[]	getDrift(int timeIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor) {
    	return model.getDrift(timeIndex, realizationAtTimeIndex, realizationPredictor);
    }

    public ImmutableRandomVariableInterface		getFactorLoading(int timeIndex, int factor, int component) {
        // Delegate to model
        return model.getFactorLoading(timeIndex, factor, component);
    }

    public RandomVariableInterface	applyStateSpaceTransform(RandomVariableInterface randomVariable) {
        // Delegate to model
        model.applyStateSpaceTransform(randomVariable);
		return randomVariable;
    }    

    /* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getNumberOfPaths()
     */
	abstract public int getNumberOfPaths();

	/* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getNumberOfFactors()
     */
	abstract public int getNumberOfFactors();
	
	/*
	 * Time discretization management
	 */
	
	/* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getTimeDiscretization()
     */
	public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}
		
	/* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getTime(int)
     */
	public double getTime(int timeIndex) {
		return timeDiscretization.getTime(timeIndex);
	}

	/* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getTimeIndex(double)
     */
	public int getTimeIndex(double time) {
		return timeDiscretization.getTimeIndex(time);
	}

}