/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Calculated the day count by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * The daycount fraction is calculated using ACT_ACT_ISDA convention, that is, the
 * daycount fraction is <i>n<sub>1</sub>/365</i> + <i>n<sub>2</sub>/366/<i>, where
 * <ul>
 * 	<li>	<i>n<sub>1</sub></i> is the number of days falling into a non-leap year,</li>
 * 	<li>	<i>n<sub>2</sub></i> is the number of days falling into a leap year,</li>
 * </ul>
 * where the start date is included in the counting and the end date is excluded in the counting.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_ACT_ACT_ISDA implements DayCountConventionInterface {

	/**
	 * Create an ACT/ACT ISDA daycount convention.
	 */
	public DayCountConvention_ACT_ACT_ISDA() {
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public double getDaycount(GregorianCalendar startDate, GregorianCalendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		return daysBetween(startDate, endDate);
	}
	


	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(GregorianCalendar startDate, GregorianCalendar endDate) {
		if(startDate.after(endDate)) return -getDaycountFraction(endDate,startDate);

		double daycountFraction = endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR);

		GregorianCalendar startDateNextYear = (GregorianCalendar)startDate.clone();
		startDateNextYear.set(Calendar.DAY_OF_YEAR, 1);
		startDateNextYear.add(Calendar.YEAR, 1);
		daycountFraction += daysBetween(startDate, startDateNextYear) / startDate.getActualMaximum(Calendar.DAY_OF_YEAR);

		GregorianCalendar endDateStartYear = (GregorianCalendar)endDate.clone();
		endDateStartYear.set(Calendar.DAY_OF_YEAR, 1);
		daycountFraction += daysBetween(endDateStartYear, endDate) / endDate.getActualMaximum(Calendar.DAY_OF_YEAR);
		
		return daycountFraction-1.0;
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
