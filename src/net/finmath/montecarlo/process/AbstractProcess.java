/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 19.01.2004
 */
package net.finmath.montecarlo.process;

import net.finmath.montecarlo.model.AbstractModelInterface;
import net.finmath.stochastic.ImmutableRandomVariableInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * This class is an abstract base class to implement a multi-dimensional multi-factor Ito process.
 * The dimension is called <code>numberOfComponents</code> here.
 * The default for <code>numberOfFactors</code> is 1.
 * 
 * This base class manages the time discretization and delegation to the model.
 * 
 * @author Christian Fries
 * @version 1.5
 */
public abstract class AbstractProcess implements AbstractProcessInterface, Cloneable {

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


	
    /*
     * Delegation to model
     */

    public void setModel(AbstractModelInterface model) {
    	if(this.model != null) throw new RuntimeException("Attemp to reuse process with a different model. This process is already associated with a model.");

    	this.model = model;
    }

    public int getNumberOfComponents() {
        return model.getNumberOfComponents();
    }
	
    public ImmutableRandomVariableInterface[]	getInitialState() {
        return model.getInitialState();
    };

    public ImmutableRandomVariableInterface[]	getDrift(int timeIndex, ImmutableRandomVariableInterface[] realizationAtTimeIndex, ImmutableRandomVariableInterface[] realizationPredictor) {
    	return model.getDrift(timeIndex, realizationAtTimeIndex, realizationPredictor);
    }

    public ImmutableRandomVariableInterface[]	getFactorLoading(int timeIndex, int component, ImmutableRandomVariableInterface[] realizationAtTimeIndex) {
        // Delegate to model
        return model.getFactorLoading(timeIndex, component, realizationAtTimeIndex);
    }

    public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariableInterface) {
        // Delegate to model
        return model.applyStateSpaceTransform(componentIndex, randomVariableInterface);
    }    


	/*
	 * Time discretization management
	 */
	
	/* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getTimeDiscretization()
     */
	@Override
    public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}
		
	/* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getTime(int)
     */
	@Override
    public double getTime(int timeIndex) {
		return timeDiscretization.getTime(timeIndex);
	}

	/* (non-Javadoc)
     * @see net.finmath.montecarlo.AbstractProcessInterface#getTimeIndex(double)
     */
	@Override
    public int getTimeIndex(double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@Override
    public abstract Object clone();

}