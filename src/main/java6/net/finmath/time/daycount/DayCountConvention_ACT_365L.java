/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import org.threeten.bp.LocalDate;

/**
 * Implementation of ACT/365L.
 *
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * The day count fraction is calculated using ACT/365L convention, that is, the
 * day count is divided by 365 if the end date is not a leap year and 366 if the end date is a leap year.
 *
 * @see DayCountConvention_ACT_365A
 * @see DayCountConvention_ACT_365
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DayCountConvention_ACT_365L extends DayCountConvention_ACT {

	/**
	 * Create an ACT/365 day count convention.
	 */
	public DayCountConvention_ACT_365L() {
	}

	@Override
	public double getDaycountFraction(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			return -getDaycountFraction(endDate,startDate);
		}

		double daysPerYear = 365.0;

		// Check endDate for leap year
		if (endDate.isLeapYear()) {
			daysPerYear = 366.0;
		}

		double daycountFraction = getDaycount(startDate, endDate) / daysPerYear;

		return daycountFraction;
	}
}
