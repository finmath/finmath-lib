/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Implementation of ACT/ACT AFB.
 * 
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 * 
 * The method is only exact, if the two calendar dates are (approximately) on the same time. A fractional day is
 * rounded to the approximately nearest day (since daylight saving is not considered, the notion of nearest may be off by one hour).
 * 
 * The day count fraction is calculated using ACT_ACT_AFB convention. The algorithm works as follows:
 * <ul>
 * 	<li>
 * 		If the interval from startDate to endDate spans more than a whole year, then
 * 		the number of whole years is subtracted from the endDate year and added to the day count fraction.
 * 	</li>
 * 	<li>
 * 		For the remaining interval the actual number of days is divided by a denominator,
 * 		where the denominator is 366.0 if February 29th is in between start and end and 365.0 otherwise.
 * 	</li>
 * </ul>
 * 
 * @author Christian Fries
 */
public class DayCountConvention_ACT_ACT_AFB extends DayCountConvention_ACT {

	/**
	 * Create an ACT/ACT FBA daycount convention.
	 */
	public DayCountConvention_ACT_ACT_AFB() {
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycountFraction(endDate,startDate);

		GregorianCalendar fractionalPeriodEnd = (GregorianCalendar)endDate.clone();
		fractionalPeriodEnd.set(Calendar.YEAR, startDate.get(Calendar.YEAR));
		if(fractionalPeriodEnd.before(startDate)) fractionalPeriodEnd.add(Calendar.YEAR, 1);
		if(fractionalPeriodEnd.get(Calendar.MONTH) == Calendar.FEBRUARY && fractionalPeriodEnd.get(Calendar.DAY_OF_MONTH) == 28 && fractionalPeriodEnd.isLeapYear(fractionalPeriodEnd.get(Calendar.YEAR))) {
			fractionalPeriodEnd.add(Calendar.DAY_OF_YEAR, 1);
		}

		double daycountFraction = endDate.get(Calendar.YEAR) - fractionalPeriodEnd.get(Calendar.YEAR);

		double fractionPeriodDenominator = 365.0;
		if(fractionalPeriodEnd.isLeapYear(fractionalPeriodEnd.get(Calendar.YEAR))) {
			GregorianCalendar feb29th = (GregorianCalendar)fractionalPeriodEnd.clone();
			feb29th.set(Calendar.MONTH, Calendar.FEBRUARY);
			feb29th.set(Calendar.DAY_OF_MONTH, 29);
			if(startDate.compareTo(feb29th) <= 0 && fractionalPeriodEnd.compareTo(feb29th) > 0) {
				fractionPeriodDenominator = 366.0;
			}
		}
		else if((new GregorianCalendar()).isLeapYear(startDate.get(Calendar.YEAR))) {
			GregorianCalendar feb29th = (GregorianCalendar)startDate.clone();
			feb29th.set(Calendar.MONTH, Calendar.FEBRUARY);
			feb29th.set(Calendar.DAY_OF_MONTH, 29);
			if(startDate.compareTo(feb29th) <= 0 && fractionalPeriodEnd.compareTo(feb29th) > 0) {
				fractionPeriodDenominator = 366.0;
			}
		}
		
		daycountFraction += getDaycount(startDate, fractionalPeriodEnd) / fractionPeriodDenominator;
		
		return daycountFraction;
	}

	@Override
	public String toString() {
		return "DayCountConvention_ACT_ACT_AFB";
	}

}
