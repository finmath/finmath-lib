/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 19.01.2004
 */
package net.finmath.montecarlo.process;

import net.finmath.montecarlo.model.ProcessModel;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class is an abstract base class to implement a multi-dimensional multi-factor Ito process.
 * The dimension is called <code>numberOfComponents</code> here.
 * The default for <code>numberOfFactors</code> is 1.
 *
 * This base class manages the time discretization and delegation to the model.
 *
 * @author Christian Fries
 * @see MonteCarloProcess The interface definition contains more details.
 * @version 1.5
 */
public abstract class MonteCarloProcessFromProcessModel implements MonteCarloProcess, Cloneable {

	private ProcessModel			model;
	private TimeDiscretization		timeDiscretization;

	/**
	 * Create a discretization scheme / a time discrete process.
	 *
	 * @param timeDiscretizationFromArray The time discretization used for the discretization scheme.
	 */
	public MonteCarloProcessFromProcessModel(TimeDiscretization timeDiscretization) {
		super();
		this.timeDiscretization	= timeDiscretization;
	}

	public abstract Object getCloneWithModifiedSeed(int seed);

	/*
	 * Delegation to model
	 */

	@Override
	public void setModel(ProcessModel model) {
		if(this.model != null) {
			throw new RuntimeException("Attempt to reuse process with a different model. This process is already associated with a model.");
		}

		this.model = model;
	}

	@Override
	public int getNumberOfComponents() {
		return model.getNumberOfComponents();
	}

	public RandomVariable[]	getInitialState() {
		return model.getInitialState();
	}

	public RandomVariable[]	getDrift(int timeIndex, RandomVariable[] realizationAtTimeIndex, RandomVariable[] realizationPredictor) {
		return model.getDrift(timeIndex, realizationAtTimeIndex, realizationPredictor);
	}

	public RandomVariable[]	getFactorLoading(int timeIndex, int component, RandomVariable[] realizationAtTimeIndex) {
		// Delegate to model
		return model.getFactorLoading(timeIndex, component, realizationAtTimeIndex);
	}

	public RandomVariable applyStateSpaceTransform(int componentIndex, RandomVariable randomVariable) {
		// Delegate to model
		return model.applyStateSpaceTransform(componentIndex, randomVariable);
	}

	public RandomVariable applyStateSpaceTransformInverse(int componentIndex, RandomVariable randomVariable) {
		// Delegate to model
		return model.applyStateSpaceTransformInverse(componentIndex, randomVariable);
	}

	/*
	 * Time discretization management
	 */

	@Override
	public TimeDiscretization getTimeDiscretization() {
		return timeDiscretization;
	}

	@Override
	public double getTime(int timeIndex) {
		if(timeIndex < 0 || timeIndex >= timeDiscretization.getNumberOfTimes()) {
			throw new ArrayIndexOutOfBoundsException("Index for process time discretization out of bounds.");
		}
		return timeDiscretization.getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	@Override
	public abstract MonteCarloProcessFromProcessModel clone();
}
