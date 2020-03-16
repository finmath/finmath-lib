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
	private final TimeDiscretization		timeDiscretization;

	/**
	 * Create a discretization scheme / a time discrete process.
	 *
	 * @param timeDiscretization The time discretization used for the discretization scheme.
	 * @param model Set the model used to generate the stochastic process. The model has to implement {@link net.finmath.montecarlo.model.ProcessModel}.
	 */
	public MonteCarloProcessFromProcessModel(final TimeDiscretization timeDiscretization, final ProcessModel model) {
		super();
		this.timeDiscretization	= timeDiscretization;
		this.model = model;
	}

	/**
	 * Create a discretization scheme / a time discrete process.
	 *
	 * @param timeDiscretization The time discretization used for the discretization scheme.
	 */
	public MonteCarloProcessFromProcessModel(final TimeDiscretization timeDiscretization) {
		super();
		this.timeDiscretization	= timeDiscretization;
	}

	public abstract Object getCloneWithModifiedSeed(int seed);

	/*
	 * Delegation to model
	 */

	/**
	 * Set the model used to generate the stochastic process.
	 * The model has to implement {@link net.finmath.montecarlo.model.ProcessModel}.
	 */
	@Override
	public void setModel(final ProcessModel model) {
		if(this.model != null && this.model != model) {
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

	public RandomVariable[]	getDrift(final int timeIndex, final RandomVariable[] realizationAtTimeIndex, final RandomVariable[] realizationPredictor) {
		return model.getDrift(this, timeIndex, realizationAtTimeIndex, realizationPredictor);
	}

	public RandomVariable[]	getFactorLoading(final int timeIndex, final int component, final RandomVariable[] realizationAtTimeIndex) {
		// Delegate to model
		return model.getFactorLoading(this, timeIndex, component, realizationAtTimeIndex);
	}

	public RandomVariable applyStateSpaceTransform(final int componentIndex, final RandomVariable randomVariable) {
		// Delegate to model
		return model.applyStateSpaceTransform(componentIndex, randomVariable);
	}

	public RandomVariable applyStateSpaceTransformInverse(final int componentIndex, final RandomVariable randomVariable) {
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
	public double getTime(final int timeIndex) {
		if(timeIndex < 0 || timeIndex >= timeDiscretization.getNumberOfTimes()) {
			throw new ArrayIndexOutOfBoundsException("Index " + timeIndex + " for process time discretization out of bounds.");
		}
		return timeDiscretization.getTime(timeIndex);
	}

	@Override
	public int getTimeIndex(final double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	@Override
	public abstract MonteCarloProcessFromProcessModel clone();
}
