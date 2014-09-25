/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Implementation of ACT/365A.
 * 
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * The day count fraction is calculated using ACT/365A convention, that is, the
 * day count is divided by 366 if February 29 lies in between startDate (including) and endDate (excluding),
 * otherwise it the day count is divided by 365.
 * 
 * @see DayCountConvention_ACT_365
 * @see DayCountConvention_ACT_365L
 * 
 * @author Christian Fries
 */
public class DayCountConvention_ACT_365A extends DayCountConvention_ACT {

	/**
	 * Create an ACT/365 day count convention.
	 */
	public DayCountConvention_ACT_365A() {
	}
	

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycountFraction(endDate,startDate);

		double daysPerYear = 365.0;
		
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		
		// Check startDate for leap year
		if(gregorianCalendar.isLeapYear(startDate.get(Calendar.YEAR))) {
			Calendar leapDayStart = new GregorianCalendar(startDate.get(Calendar.YEAR), Calendar.FEBRUARY, 29);
			if(!startDate.after(leapDayStart) && endDate.after(leapDayStart)) daysPerYear = 366.0;
		}

		// Check endDate for leap year
		if(gregorianCalendar.isLeapYear(endDate.get(Calendar.YEAR))) {
			Calendar leapDayEnd = new GregorianCalendar(endDate.get(Calendar.YEAR), Calendar.FEBRUARY, 29);
			if(!startDate.after(leapDayEnd) && endDate.after(leapDayEnd)) daysPerYear = 366.0;
		}

		// Check in-between years for leap year
		for(int year = startDate.get(Calendar.YEAR)+1; year < endDate.get(Calendar.YEAR); year++) if(gregorianCalendar.isLeapYear(year)) daysPerYear = 366.0;
		
		double daycountFraction = getDaycount(startDate, endDate) / daysPerYear;

		return daycountFraction;
	}
}
