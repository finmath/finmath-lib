/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Implementation of ACT/ACT ISDA.
 * 
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * <p>
 * The day count fraction is calculated using ACT_ACT_ISDA convention, that is, the
 * day count fraction is <i>n<sub>1</sub>/365</i> + <i>n<sub>2</sub>/366<i>, where
 * <ul>
 * 	<li>	<i>n<sub>1</sub></i> is the number of days falling into a non-leap year,</li>
 * 	<li>	<i>n<sub>2</sub></i> is the number of days falling into a leap year,</li>
 * </ul>
 * where the start date is included in the counting and the end date is excluded in the counting.
 * This means that from 31.12.2014 to 01.01.2015 the day count fraction is 1/366 since 2014 is a leap year.
 * </p>
 * 
 * <p>
 * The class passed that standard benchmark test in the IDSA document, see {@link net.finmath.tests.time.daycount.DayCountConventionTest}.
 * </p>
 *
 * <p>
 * The class can be constructed using isCountLastDayNotFirst = true, where the above behavior is changed
 * In this case the start date is excluded in the counting and the end date is included in the counting.
 * This means that from 31.12.2014 to 01.01.2015 the day count fraction is 1/365 since 2015 is not a leap year.
 * </p>
 * 
 * @author Christian Fries
 */
public class DayCountConvention_ACT_ACT_ISDA extends DayCountConvention_ACT implements DayCountConventionInterface {

	private boolean isCountLastDayNotFirst = false;
	
	/**
	 * Create an ACT/ACT ISDA daycount convention.
	 */
	public DayCountConvention_ACT_ACT_ISDA() {
	}

	/**
	 * Create an ACT/ACT ISDA daycount convention.
	 * 
	 * @param isCountLastDayNotFirst If this value is false (default), the days are counted including the first day and excluding the last day. If this field is true, the days are counted excluding the first day and including the last day.
	 */
	public DayCountConvention_ACT_ACT_ISDA(boolean isCountFirstDayNotLast) {
		super();
		this.isCountLastDayNotFirst = isCountFirstDayNotLast;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycountFraction(endDate,startDate);

		/*
		 * Number of whole years between start and end. If start and end fall in the same year, this is -1 (there will be a double counting of 1 year below if start < end).
		 */
		double daycountFraction = endDate.get(Calendar.YEAR) - startDate.get(Calendar.YEAR) - 1.0;

		/*
		 * Fraction from start to the end of start's year
		 */
		GregorianCalendar startDateNextYear = (GregorianCalendar)startDate.clone();
		startDateNextYear.set(Calendar.DAY_OF_YEAR, 1);
		startDateNextYear.add(Calendar.YEAR, 1);
		if(isCountLastDayNotFirst) startDateNextYear.add(Calendar.DAY_OF_YEAR, -1);

		daycountFraction += getDaycount(startDate, startDateNextYear) / startDate.getActualMaximum(Calendar.DAY_OF_YEAR);

		/*
		 * Fraction from beginning of end's year to end
		 */
		GregorianCalendar endDateStartYear = (GregorianCalendar)endDate.clone();
		endDateStartYear.set(Calendar.DAY_OF_YEAR, 1);
		if(isCountLastDayNotFirst) endDateStartYear.add(Calendar.DAY_OF_YEAR, -1);

		daycountFraction += getDaycount(endDateStartYear, endDate) / endDate.getActualMaximum(Calendar.DAY_OF_YEAR);
		
		return Math.max(daycountFraction,0.0);
	}

	@Override
	public String toString() {
		return "DayCountConvention_ACT_ACT_ISDA [isCountLastDayNotFirst="
				+ isCountLastDayNotFirst + "]";
	}

}
