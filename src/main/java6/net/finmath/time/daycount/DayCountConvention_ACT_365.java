/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import org.threeten.bp.LocalDate;

/**
 * Implementation of ACT/365.
 *
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * The day count fraction is calculated using ACT/365 convention, that is, the
 * day count is divided by 365.
 *
 * <ul>
 * 	<li>
 * 		The method {@link #getDaycountFraction(LocalDate, LocalDate) getDaycountFraction} corresponds to the implementation of the "ACT/365 method" of Excel function YEARFRAC, i.e., YEARFRAC(startDate,endDate,3).
 * 	</li>
 * </ul>
 *
 * @author Christian Fries
 */
public class DayCountConvention_ACT_365 extends DayCountConvention_ACT {

	/**
	 * Create an ACT/365 day count convention.
	 */
	public DayCountConvention_ACT_365() {
	}


	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			return -getDaycountFraction(endDate,startDate);
		}

		double daycountFraction = getDaycount(startDate, endDate) / 365.0;

		return daycountFraction;
	}
}
