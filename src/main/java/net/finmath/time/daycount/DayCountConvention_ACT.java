/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.time.LocalDate;

/**
 * Base class which calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class DayCountConvention_ACT implements DayCountConvention {

	/**
	 * Create an ACT day count convention.
	 */
	public DayCountConvention_ACT() {
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConvention#getDaycount(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycount(final LocalDate startDate, final LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			return -getDaycount(endDate,startDate);
		}

		return daysBetween(startDate, endDate);
	}

	/**
	 * Returns the number of days, between two dates.
	 *
	 * A fractional day is rounded to the approximately nearest day.
	 *
	 * The formula implemented is
	 * <code>
	 *
	 * (endDate.toEpochDay() - startDate.toEpochDay());
	 * </code>
	 *
	 * @param startDate The start date of the interval.
	 * @param endDate The end date of the interval.
	 * @return Number of days between startDate and endDate.
	 */
	public static double daysBetween(final LocalDate startDate, final LocalDate endDate) {
		return (endDate.toEpochDay() - startDate.toEpochDay());
	}
}
