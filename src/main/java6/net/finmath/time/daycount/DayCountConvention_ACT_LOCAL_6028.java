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
 * A fractional day is rounded to the approximately nearest day 
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
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(org.joda.time.LocalDate, org.joda.time.LocalDate)
	 */
	@Override
	public double getDaycount(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) return -getDaycount(endDate,startDate);

		return daysBetween(startDate, endDate);
	}
	
	/**
	 * Returns the number of days, between two Calendar dates.
	 * 
 *  A fractional day is
	 * rounded to the approximately nearest day 
	 * 
	 * The formula implemented is
	 * <code>
	 * Math.round( ((double)(endDate.getTimeInMillis()-startDate.getTimeInMillis())) / 1000.0 / 60.0 / 60.0 / 24)
	 * </code>
	 * 
	 * <p>
	 * For more accurate calculation consider org.joda.time.Days.
	 * </p>
	 * 
	 * <p>
	 * However, the method is correct if each calendar is on the same time and they differ only by days.
	 * </p>
	 * 
	 * @param startDate The start date of the interval.
	 * @param endDate The end date of the interval.
	 * @return Number of days between startDate and endDate.
	 */
	public static double daysBetween(LocalDate startDate, LocalDate endDate) {
		return Days.daysBetween(startDate, endDate).getDays();
	}
}
