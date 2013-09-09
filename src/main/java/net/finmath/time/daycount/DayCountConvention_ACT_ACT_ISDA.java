/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * The daycount fraction is calculated using ACT_ACT_ISDA convention, that is, the
 * daycount fraction is <i>n<sub>1</sub>/365</i> + <i>n<sub>2</sub>/366<i>, where
 * <ul>
 * 	<li>	<i>n<sub>1</sub></i> is the number of days falling into a non-leap year,</li>
 * 	<li>	<i>n<sub>2</sub></i> is the number of days falling into a leap year,</li>
 * </ul>
 * where the start date is included in the counting and the end date is excluded in the counting.
 * 
 * This means that from 31.12.2014 to 01.01.2015 the daycount fraction is 1/366 since 2014 is a leap year.
 * 
 * The class passed that standard benchmark test in the IDSA document, see {@link net.finmath.tests.time.daycount.DayCountConventionTest}.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_ACT_ACT_ISDA extends DayCountConvention_ACT implements DayCountConventionInterface {

	/**
	 * Create an ACT/ACT ISDA daycount convention.
	 */
	public DayCountConvention_ACT_ACT_ISDA() {
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
		daycountFraction += getDaycount(startDate, startDateNextYear) / startDate.getActualMaximum(Calendar.DAY_OF_YEAR);

		GregorianCalendar endDateStartYear = (GregorianCalendar)endDate.clone();
		endDateStartYear.set(Calendar.DAY_OF_YEAR, 1);
		daycountFraction += getDaycount(endDateStartYear, endDate) / endDate.getActualMaximum(Calendar.DAY_OF_YEAR);
		
		return daycountFraction-1.0;
	}

	@Override
	public String toString() {
		return "DayCountConvention_ACT_ACT_ISDA []";
	}
}
