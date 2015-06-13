/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Implementation of NL/365.
 * 
 * Calculates the day count by calculating the actual number of days between startDate (excluding) and endDate (including), excluding a leap day (February 29th) if present.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * The day count fraction is calculated using NL/365 convention, that is, the
 * day count is divided by 365.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_NL_365 implements DayCountConventionInterface {

	/**
	 * Create an NL/365 day count convention.
	 */
	public DayCountConvention_NL_365() {
	}

	@Override
	public double getDaycount(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		// Get actual number of days
		double daycount = DayCountConvention_ACT.daysBetween(startDate, endDate);

		/*
		 * Remove leap days, if any.
		 */
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		for(int year = startDate.get(Calendar.YEAR) ; year <= endDate.get(Calendar.YEAR); year++) {
			if(gregorianCalendar.isLeapYear(year)) {
				Calendar leapDay = new GregorianCalendar(year, Calendar.FEBRUARY, 29);
				if(startDate.before(leapDay) && !endDate.before(leapDay)) daycount -= 1.0;
			}
		}

		if(daycount < 0.0) throw new AssertionError("Daycount is negative for startDate not after endDate.");

		return daycount;
	}

	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycountFraction(endDate,startDate);

		double daycountFraction = getDaycount(startDate, endDate) / 365.0;

		return daycountFraction;
	}
}
