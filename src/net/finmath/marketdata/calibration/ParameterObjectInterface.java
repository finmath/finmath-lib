/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata.calibration;

/**
 * An objects having a dependence on a parameter (double[]).
 * The state of the objects is encoded in the parameter. It can be read or set.
 * 
 * Note that the parameter may alter the state of multiple depending objects (e.g. referenced members).
 * 
 * @author Christian Fries
 */
public interface ParameterObjectInterface {

	/**
	 * Get the current parameter associated with the state of the objects.
	 * 
	 * @return The parameter.
	 */
	public double[] getParameter();

	/**
	 * Set the current parameter and change the state of the objects.
	 * 
	 * @param parameter The parameter associated with the new state of the objects.
	 */
	public void setParameter(double[] parameter);
	
}
