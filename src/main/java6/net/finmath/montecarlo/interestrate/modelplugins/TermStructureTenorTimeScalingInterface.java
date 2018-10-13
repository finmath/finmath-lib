/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 04.02.2017
 */

package net.finmath.montecarlo.interestrate.modelplugins;

/**
 * @author Christian Fries
 *
 * @version 1.0
 */
public interface TermStructureTenorTimeScalingInterface {

	double getScaledTenorTime(double periodStart, double periodEnd);

	/**
	 * Create a new object constructed from a clone of this time scaling, where some parameters have been modified.
	 *
	 * @param parameters The set of new parameters.
	 * @return A new object constructed from a clone of this time scaling, where some parameters have been modified.
	 */
	TermStructureTenorTimeScalingInterface getCloneWithModifiedParameters(double[] parameters);

	/**
	 * @return The parameter set representing the state of this object.
	 */
	double[] getParameter();

	/**
	 * @return A clone of this object.
	 */
	TermStructureTenorTimeScalingInterface clone();
}
