/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.dayount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Calculated the day count as by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * For more accurate calculation consider org.joda.time.Days.
 * 
 * However, the method is correct if each calendar is on the same time and they differ only by days.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_ACT_ACT_ISDA implements DayCountConventionInterface {

	/**
	 * Create a 30/360 ISDA daycount convention.
	 */
	public DayCountConvention_ACT_ACT_ISDA() {
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.dayount.DayCountConventionInterface#getDaycount(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public double getDaycount(GregorianCalendar startDate, GregorianCalendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		return daysBetween(startDate, endDate);
	}
	


	/* (non-Javadoc)
	 * @see net.finmath.time.dayount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(GregorianCalendar startDate, GregorianCalendar endDate) {
		if(startDate.after(endDate)) return -getDaycountFraction(endDate,startDate);

		double daysInYears = 0.0;

		GregorianCalendar date = (GregorianCalendar)startDate.clone();
		while(date.get(Calendar.YEAR) <= endDate.get(Calendar.YEAR)) {
			if(date.isLeapYear(date.get(Calendar.YEAR)))	daysInYears += 366.0;
			else											daysInYears += 365.0;
			date.add(Calendar.YEAR, 1);
		}
		
		return getDaycount(startDate, endDate) / daysInYears;
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
