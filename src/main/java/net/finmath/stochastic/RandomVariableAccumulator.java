/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 29.06.2013
 */
package net.finmath.stochastic;


/**
 * The interface implemented by a mutable random variable accumulator.
 * An object of this class accumulates other random variables.
 * The classical application is the accumulation of discounted cash flows.
 *
 * @author Christian Fries
 * @version 1.3
 */
public interface RandomVariableAccumulator extends RandomVariable {

	void accumulate(RandomVariable randomVariable);
	void accumulate(double time, RandomVariable randomVariable);

	RandomVariable get();
	RandomVariable get(double fromTime, double toTime);
}
