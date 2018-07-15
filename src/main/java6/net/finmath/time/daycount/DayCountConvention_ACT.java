/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import org.threeten.bp.LocalDate;

/**
 * Base class which calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * @author Christian Fries
 */
public abstract class DayCountConvention_ACT implements DayCountConventionInterface {

	/**
	 * Create an ACT day count convention.
	 */
	public DayCountConvention_ACT() {
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(org.threeten.bp.LocalDate, org.threeten.bp.LocalDate)
	 */
	@Override
	public double getDaycount(LocalDate startDate, LocalDate endDate) {
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
	public static double daysBetween(LocalDate startDate, LocalDate endDate) {
		return (endDate.toEpochDay() - startDate.toEpochDay());
	}
}
