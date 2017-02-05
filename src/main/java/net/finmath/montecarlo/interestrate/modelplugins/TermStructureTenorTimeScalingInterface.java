/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 04.02.2017
 */

package net.finmath.montecarlo.interestrate.modelplugins;

/**
 * @author Christian Fries
 *
 */
public interface TermStructureTenorTimeScalingInterface {

	double getScaledTenorTime(double periodStart, double periodEnd);

	/**
	 * @param parameters
	 * @return
	 */
	TermStructureTenorTimeScalingInterface getCloneWithModifiedParameters(double[] parameters);

	/**
	 * @return
	 */
	double[] getParameter();

	/**
	 * @return
	 */
	TermStructureTenorTimeScalingInterface clone();
}
