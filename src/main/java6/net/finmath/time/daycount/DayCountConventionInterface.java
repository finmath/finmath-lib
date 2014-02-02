/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;

/**
 * Interface for various day count conventions.
 * 
 * Classes implementing this interface have to implement the methods
 * {@link #getDaycount(Calendar, Calendar)} and {@link #getDaycountFraction(Calendar, Calendar)}.
 * 
 * Classes implementing these methods then provide day counting and day count fractions for
 * a given interval on a {@link java.util.GregorianCalendar}.
 * 
 * @author Christian Fries
 */
public interface DayCountConventionInterface {

	/**
	 * Return the number of days between startDate and endDate given the
	 * specific daycount convention.
	 * 
	 * @param startDate The start date given as a {@link java.util.Calendar}.
	 * @param endDate The end date given as a {@link java.util.Calendar}.
	 * @return The number of days within the given period.
	 */
	public abstract double getDaycount(Calendar startDate, Calendar endDate);

	/**
	 * Return the daycount fraction corresponding to the period from startDate to endDate given the
	 * specific daycount convention.
	 * 
	 * @param startDate The start date given as a {@link java.util.Calendar}.
	 * @param endDate The end date given as a {@link java.util.Calendar}.
	 * @return The daycount fraction corresponding to the given period.
	 */
	public abstract double getDaycountFraction(Calendar startDate, Calendar endDate);
	
}