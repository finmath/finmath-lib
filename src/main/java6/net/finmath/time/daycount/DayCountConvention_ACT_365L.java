/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Implementation of ACT/365L.
 * 
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * The day count fraction is calculated using ACT/365L convention, that is, the
 * day count is divided by 365 if the end date is not a leap year and 366 if the end date is a leap year.
 * 
 * @see DayCountConvention_ACT_365A
 * @see DayCountConvention_ACT_365
 * 
 * @author Christian Fries
 */
public class DayCountConvention_ACT_365L extends DayCountConvention_ACT {

	/**
	 * Create an ACT/365 day count convention.
	 */
	public DayCountConvention_ACT_365L() {
	}
	
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycountFraction(endDate,startDate);

		double daysPerYear = 365.0;
		
		// Check endDate for leap year
		GregorianCalendar gregorianCalendar = new GregorianCalendar();
		if(gregorianCalendar.isLeapYear(endDate.get(Calendar.YEAR))) daysPerYear = 366.0;
		
		double daycountFraction = getDaycount(startDate, endDate) / daysPerYear;

		return daycountFraction;
	}
}
