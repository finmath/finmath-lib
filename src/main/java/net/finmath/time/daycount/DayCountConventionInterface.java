/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.GregorianCalendar;

/**
 * Interface implemented by various daycount conventions.
 * Classes implementing this interface provide daycounting and daycount fractions for
 * a given interval on a {@link java.util.GregorianCalendar}.
 * 
 * @author Christian Fries
 */
public interface DayCountConventionInterface {

	/**
	 * Return the number of days between startDate and endDate given the
	 * specific daycount convention.
	 * 
	 * @param startDate The start date given as a {@link java.util.GregorianCalendar}.
	 * @param endDate The end date given as a {@link java.util.GregorianCalendar}.
	 * @return The number of days within the given period.
	 */
	public abstract double getDaycount(GregorianCalendar startDate, GregorianCalendar endDate);

	/**
	 * Return the daycount fraction corresponding to the period from startDate to endDate given the
	 * specific daycount convention.
	 * 
	 * @param startDate The start date given as a {@link java.util.GregorianCalendar}.
	 * @param endDate The end date given as a {@link java.util.GregorianCalendar}.
	 * @return The daycount fraction corresponding to the given period.
	 */
	public abstract double getDaycountFraction(GregorianCalendar startDate, GregorianCalendar endDate);
}