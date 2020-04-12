/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata2.calibration;

import net.finmath.stochastic.RandomVariable;

/**
 * An objects having a dependence on a parameter (double[]).
 * The state of the objects is encoded in the parameter. It can be read or set.
 *
 * Note that the parameter may alter the state of multiple depending objects (e.g. referenced members).
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface ParameterObject {

	/**
	 * Get the current parameter associated with the state of the objects.
	 *
	 * @return The parameter.
	 */
	RandomVariable[] getParameter();

	/**
	 * Create a clone with a modified parameter.
	 *
	 * @param value The new parameter.
	 * @return A clone with an otherwise modified parameter.
	 * @throws CloneNotSupportedException Thrown, when the curve could not be cloned.
	 */
	ParameterObject getCloneForParameter(RandomVariable[] value) throws CloneNotSupportedException;

	/**
	 * Set the current parameter and change the state of the objects.
	 *
	 * @param parameter The parameter associated with the new state of the objects.
	 */
	@Deprecated
	void setParameter(RandomVariable[] parameter);

}
