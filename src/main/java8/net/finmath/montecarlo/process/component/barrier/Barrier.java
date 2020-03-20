package net.finmath.montecarlo.process.component.barrier;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.RandomVariableFromDoubleArray;
import net.finmath.stochastic.RandomVariable;

/**
 * The interface describes how an barrier has to be specified for the generation of a process (see LogNormalProcessWithBarrierStrategy).
 *
 * @author Christian Fries
 * @version 1.0
 * @since finmath-lib 4.1.0
 */
public interface Barrier {

	/**
	 * The barrier direction, i.e. a (stochastic) projection vector for the components)
	 *
	 * @param timeIndex Time index associated with the model time discretization.
	 * @param randomVariable Model process realization at timeIndex.
	 * @return Direction of barrier.
	 */
	RandomVariableFromDoubleArray[]	getBarrierDirection(int timeIndex, RandomVariable[] randomVariable);

	/**
	 * The barrier level
	 *
	 * @param timeIndex Time index associated with the model time discretization.
	 * @param randomVariable Model process realization at timeIndex.
	 * @return Level of barrier.
	 * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
	 */
	RandomVariable		getBarrierLevel(int timeIndex, RandomVariable[] randomVariable) throws CalculationException;

	/**
	 * @return Returns true, if the barrier is an upper barrier. Returns false, if the barrier is an lower barrier.
	 */
	boolean isUpperBarrier();
}
