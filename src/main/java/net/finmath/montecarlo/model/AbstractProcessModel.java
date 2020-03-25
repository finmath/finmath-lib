package net.finmath.montecarlo.model;

import java.time.LocalDateTime;

import net.finmath.montecarlo.process.MonteCarloProcess;
import net.finmath.stochastic.RandomVariable;

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

	/**
	 * Returns the initial value of the model.
	 *
	 * @param process The discretization process generating this model. The process provides call backs for TimeDiscretization and allows calls to getProcessValue for timeIndices less or equal the given one.
	 * @return The initial value of the model.
	 */
	public RandomVariable[] getInitialValue(MonteCarloProcess process) {
		final RandomVariable[] initialState = getInitialState(process);

		final RandomVariable[] value = new RandomVariable[initialState.length];
		for(int i= 0; i<value.length; i++) {
			value[i] = applyStateSpaceTransform(process, 0, i, initialState[i]);
		}

		return value;
	}

	@Override
	public LocalDateTime getReferenceDate() {
		throw new UnsupportedOperationException("This model does not provide a reference date. Reference dates will be mandatory in a future version.");
	}
}
