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
public interface RandomVariableAccumulatorInterface extends RandomVariableInterface {

	void accumulate(RandomVariableInterface randomVariable);
	void accumulate(double time, RandomVariableInterface randomVariable);

	RandomVariableInterface get();
	RandomVariableInterface get(double fromTime, double toTime);
}