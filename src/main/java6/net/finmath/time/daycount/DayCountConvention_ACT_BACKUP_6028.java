/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import org.joda.time.Days;
import org.joda.time.LocalDate;

/**
 * Base class which calculates the day count by calculating the actual number of days between startDate and endDate.
 * 
<<<<<<< HEAD
 * A fractional day is rounded to the approximately nearest day 
=======
 * A fractional day is rounded to the approximately nearest day.
>>>>>>> finmath-lib/master
 * 
 * @author Christian Fries
 */
public abstract class DayCountConvention_ACT implements DayCountConventionInterface {

	/**
	 * Create an ACT day count convention.
	 */
	public DayCountConvention_ACT() {
	}
	
	/* (non-Javadoc)
<<<<<<< HEAD
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(org.joda.time.LocalDate, org.joda.time.LocalDate)
=======
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.time.LocalDate, java.time.LocalDate)
>>>>>>> finmath-lib/master
	 */
	@Override
	public double getDaycount(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) return -getDaycount(endDate,startDate);

		return daysBetween(startDate, endDate);
	}
	
	/**
	 * Returns the number of days, between two dates.
	 * 
<<<<<<< HEAD
 *  A fractional day is
	 * rounded to the approximately nearest day 
=======
	 * A fractional day is rounded to the approximately nearest day.
>>>>>>> finmath-lib/master
	 * 
	 * The formula implemented is
	 * <code>
	 * 
	 * (endDate.toEpochDay() - startDate.toEpochDay());
	 * </code>
	 * 
	 * @param startDate The start date of the interval.
	 * @param endDate The end date of the interval.
	 * @return Number of days between startDate and endDate.
	 */
	public static double daysBetween(LocalDate startDate, LocalDate endDate) {
		return Days.daysBetween(startDate, endDate).getDays();
	}
}
