/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.util.Calendar;

/**
 * @author Christian Fries
 *
 */
public interface TenorInterface {

	/**
	 * @return The reference date of this tenor, i.e., the date mapped to 0.0
	 */
	public abstract Calendar getReferenceDate();

	/**
	 * Returns the date for the given time index.
	 * 
	 * @param timeIndex Time index
	 * @return Returns the date for a given time index.
	 */
	public abstract Calendar getDate(int timeIndex);

	/**
	 * Returns the day count fraction for the period form timeIndex to to timeIndex+1.
	 * 
	 * @param timeIndex Time index
	 * @return Returns the day count fraction.
	 */
	public abstract double getDaycountFraction(int timeIndex);
}