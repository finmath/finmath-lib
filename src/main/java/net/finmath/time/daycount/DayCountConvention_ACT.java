/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.GregorianCalendar;

/**
 * Base class which calculates the day count by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
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
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public double getDaycount(GregorianCalendar startDate, GregorianCalendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		return daysBetween(startDate, endDate);
	}
	
	/**
	 * Returns the number of days, between two Calendar dates.
	 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
	 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
	 * 
	 * For more accurate calculation consider org.joda.time.Days.
	 * 
	 * However, the method is correct if each calendar is on the same time and they differ only by days.
	 * 
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	private static double daysBetween(GregorianCalendar startDate, GregorianCalendar endDate) {
		return Math.round( ((double)(endDate.getTimeInMillis()-startDate.getTimeInMillis())) / 1000.0 / 60.0 / 60.0 / 24);
	}
}
