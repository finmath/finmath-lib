/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 19.01.2004
 */
package net.finmath.montecarlo.process;

import net.finmath.montecarlo.model.AbstractModelInterface;
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
 * @see AbstractProcessInterface The interface definition contains more details.
 * @version 1.5
 */
public abstract class AbstractProcess implements AbstractProcessInterface, Cloneable {

    private AbstractModelInterface			model;
    private TimeDiscretizationInterface		timeDiscretization;

	/**
	 * Create a discretization scheme / a time discrete process.
	 * 
	 * @param timeDiscretization The time discretization used for the discretization scheme.
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
    	if(this.model != null) throw new RuntimeException("Attempt to reuse process with a different model. This process is already associated with a model.");

    	this.model = model;
    }

    public int getNumberOfComponents() {
        return model.getNumberOfComponents();
    }
	
    public RandomVariableInterface[]	getInitialState() {
        return model.getInitialState();
    };

    public RandomVariableInterface[]	getDrift(int timeIndex, RandomVariableInterface[] realizationAtTimeIndex, RandomVariableInterface[] realizationPredictor) {
    	return model.getDrift(timeIndex, realizationAtTimeIndex, realizationPredictor);
    }

    public RandomVariableInterface[]	getFactorLoading(int timeIndex, int component, RandomVariableInterface[] realizationAtTimeIndex) {
        // Delegate to model
        return model.getFactorLoading(timeIndex, component, realizationAtTimeIndex);
    }

    public RandomVariableInterface applyStateSpaceTransform(int componentIndex, RandomVariableInterface randomVariable) {
        // Delegate to model
        return model.applyStateSpaceTransform(componentIndex, randomVariable);
    }

    public RandomVariableInterface applyStateSpaceTransformInverse(int componentIndex, RandomVariableInterface randomVariable) {
        // Delegate to model
        return model.applyStateSpaceTransformInverse(componentIndex, randomVariable);
    }

	/*
	 * Time discretization management
	 */
	
	@Override
    public TimeDiscretizationInterface getTimeDiscretization() {
		return timeDiscretization;
	}
		
	@Override
    public double getTime(int timeIndex) {
		if(timeIndex < 0 || timeIndex >= timeDiscretization.getNumberOfTimes()) throw new ArrayIndexOutOfBoundsException("Index for process time discretization out of bounds.");
		return timeDiscretization.getTime(timeIndex);
	}

	@Override
    public int getTimeIndex(double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	@Override
    public abstract AbstractProcess clone();
}