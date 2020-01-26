package net.finmath.montecarlo.model;

import java.time.LocalDateTime;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.TimeDiscretization;

/**
 * This class is an abstract base class to implement a model provided to an MonteCarloProcessFromProcessModel.
 *
 * Manages the delegation to MonteCarloProcess.
 *
 * For details see {@link net.finmath.montecarlo.model.ProcessModel}.
 *
 * @author Christian Fries
 * @see ProcessModel The interface definition contains more details.
 * @version 1.3
 */
public abstract class AbstractProcessModel implements ProcessModel {

	private transient MonteCarloProcess process;

	/**
	 * Returns the initial value of the model.
	 *
	 * @return The initial value of the model.
	 */
	public RandomVariable[] getInitialValue() {
		final RandomVariable[] initialState = getInitialState();

		final RandomVariable[] value = new RandomVariable[initialState.length];
		for(int i= 0; i<value.length; i++) {
			value[i] = applyStateSpaceTransform(i,initialState[i]);
		}

		return value;
	}

	/*
	 * Delegation to process (numerical scheme)
	 */

	@Override
	public void setProcess(final MonteCarloProcess process) {
		this.process = process;
	}

	@Override
	public MonteCarloProcess getProcess() {
		return process;
	}

	@Override
	public int getNumberOfFactors() {
		return process.getNumberOfFactors();
	}

	/**
	 * @param timeIndex The time index of evaluation time (using this models time discretization)
	 * @param componentIndex The component of the process vector
	 * @return Process realization as a random variable
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 * @see net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel#getProcessValue(int, int)
	 */
	public RandomVariable getProcessValue(final int timeIndex, final int componentIndex) throws CalculationException {
		return process.getProcessValue(timeIndex, componentIndex);
	}

	/**
	 * @param timeIndex The time index of evaluation time (using this models time discretization)
	 * @return A random variable representing the Monte-Carlo probabilities.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 * @see net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel#getMonteCarloWeights(int)
	 */
	public RandomVariable getMonteCarloWeights(final int timeIndex) throws CalculationException {
		return process.getMonteCarloWeights(timeIndex);
	}

	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}

	/**
	 * Get the time discretization of the model (simulation time).
	 * @return The time discretization of the model (simulation time).
	 * @see net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel#getTimeDiscretization()
	 */
	@Override
	public final TimeDiscretization getTimeDiscretization() {
		return process.getTimeDiscretization();
	}

	/**
	 * Return the simulation time for a given time index.
	 * @param timeIndex Time index
	 * @return Returns the time for a given time index.
	 * @see net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel#getTime(int)
	 */
	public final double getTime(final int timeIndex) {
		return process.getTime(timeIndex);
	}

	/**
	 * Return the time index associated for the given simulation time.
	 * @param time A given time.
	 * @return The time index corresponding to the given time.
	 * @see net.finmath.montecarlo.process.MonteCarloProcessFromProcessModel#getTimeIndex(double)
	 */
	public final int getTimeIndex(final double time) {
		return process.getTimeIndex(time);
	}
}
