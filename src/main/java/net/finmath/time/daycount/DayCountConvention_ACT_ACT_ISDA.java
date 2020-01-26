/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;

/**
 * Implementation of ACT/ACT ISDA.
 *
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * The day count fraction is calculated using ACT_ACT_ISDA convention, that is, the
 * day count fraction is <i>n<sub>1</sub>/365</i> + <i>n<sub>2</sub>/366</i>, where
 * <ul>
 * 	<li>	<i>n<sub>1</sub></i> is the number of days falling into a non-leap year,</li>
 * 	<li>	<i>n<sub>2</sub></i> is the number of days falling into a leap year,</li>
 * </ul>
 * where the start date is included in the counting and the end date is excluded in the counting.
 * This means that from 31.12.2014 to 01.01.2015 the day count fraction is 1/366 since 2014 is a leap year.
 *
 * <p>
 * The class passed that standard benchmark test in the IDSA document see net.finmath.tests.time.daycount.DayCountConventionTest
 * </p>
 *
 * <p>
 * The class can be constructed using isCountLastDayNotFirst = true, which will change its behavior compared to above.
 * In this case the start date is excluded in the counting and the end date is included in the counting.
 * This means that from 31.12.2014 to 01.01.2015 the day count fraction is 1/365 since 2015 is not a leap year.
 * </p>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DayCountConvention_ACT_ACT_ISDA extends DayCountConvention_ACT implements Serializable {

	private static final long serialVersionUID = -3351464849368249181L;

	private final boolean isCountLastDayNotFirst;

	/**
	 * Create an ACT/ACT ISDA day count convention.
	 *
	 * @param isCountFirstDayNotLast If this value is false (default), the days are counted including the first day and excluding the last day. If this field is true, the days are counted excluding the first day and including the last day.
	 */
	public DayCountConvention_ACT_ACT_ISDA(final boolean isCountFirstDayNotLast) {
		super();
		isCountLastDayNotFirst = isCountFirstDayNotLast;
	}

	/**
	 * Create an ACT/ACT ISDA day count convention.
	 */
	public DayCountConvention_ACT_ACT_ISDA() {
		this(false);
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConvention#getDaycountFraction(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycountFraction(final LocalDate startDate, final LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			return -getDaycountFraction(endDate,startDate);
		}

		/*
		 * Number of whole years between start and end, excluding start's year and excluding end's year.
		 *
		 * If start and end fall in the same year, this is -1 (there will be a double counting of 1 year below if start < end).
		 * If start and end fall in separate but consecutive years, this is 0 (for start and end the fractional parts are counted individually below).
		 */
		double daycountFraction = endDate.getYear() - startDate.getYear() - 1.0;

		/*
		 * Fraction from start to the end of start's year
		 */
		LocalDate startDateNextYear = LocalDate.of(startDate.getYear()+1,Month.JANUARY,1);

		if(isCountLastDayNotFirst) {
			startDateNextYear = startDateNextYear.minusDays(1);
		}

		daycountFraction += getDaycount(startDate, startDateNextYear) / startDate.lengthOfYear();

		/*
		 * Fraction from beginning of end's year to end
		 */
		LocalDate endDateStartYear = LocalDate.of(endDate.getYear(), Month.JANUARY, 1);
		if (isCountLastDayNotFirst) {
			endDateStartYear = endDateStartYear.minusDays(1);
		}


		daycountFraction += getDaycount(endDateStartYear, endDate) / endDate.lengthOfYear();

		return Math.max(daycountFraction,0.0);
	}

	@Override
	public String toString() {
		return "DayCountConvention_ACT_ACT_ISDA [isCountLastDayNotFirst="
				+ isCountLastDayNotFirst + "]";
	}

}
