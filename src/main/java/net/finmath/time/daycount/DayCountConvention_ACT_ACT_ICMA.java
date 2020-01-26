/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;

import net.finmath.time.Period;

/**
 * Implementation of ACT/ACT ICMA.
 *
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * The day count fraction is calculated using ACT_ACT_ICMA convention, that is, the
 * day count fraction is <i>n/(f*m)</i>, where
 * <ul>
 * 	<li>	<i>n</i> is the number of days between period start and the given day,</li>
 * 	<li>	<i>m</i> is the number of days between period start and period end,</li>
 * 	<li>	<i>f</i> is the number of periods within a year</li>
 * </ul>
 * where the start date is included in the counting and the end date is excluded in the counting.
 *
 * In contrast to other daycount conventions this daycount conventions requires information on an
 * associated schedule of periods. It depends on the definition of period start and period end
 * and on the frequency.
 *
 * This is implemented by providing an object implementing an <code>net.finmath.time.Period</code>
 * upon construction.
 *
 * <p>
 * The class can be constructed using isCountLastDayNotFirst = true, which will change its behavior compared to above.
 * In this case the start date is excluded in the counting and the end date is included in the counting.
 * This means that from 31.12.2014 to 01.01.2015 the day count fraction is 1/365 since 2015 is not a leap year.
 * </p>
 *
 * Note: The daycount method {@link DayCountConvention_ACT_ACT_ISDA} is a special case of this method
 * for frequency being one, having annual periods from January 1st to December 31st.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DayCountConvention_ACT_ACT_ICMA extends DayCountConvention_ACT implements Serializable {

	private static final long serialVersionUID = -6104391125796839417L;

	private final ArrayList<Period>	periods;
	private final int				frequency;

	/**
	 * Create an ACT/ACT ICMA day count convention.
	 *
	 * @param periods A sorted list of periods defining the periods. From the periods, only the period end dates are used as interval boundaries, except for the start date of the first interval, where the period start date is used.
	 * @param frequency The frequency, i.e., the number of periods which make up a year.
	 */
	public DayCountConvention_ACT_ACT_ICMA(final ArrayList<Period> periods, final int frequency) {
		super();
		this.periods	= periods;
		this.frequency	= frequency;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConvention#getDaycountFraction(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycountFraction(final LocalDate startDate, final LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			return -getDaycountFraction(endDate,startDate);
		}

		int periodIndexEndDate = Collections.binarySearch(periods, new Period(endDate, endDate, endDate, endDate));
		int periodIndexStartDate = Collections.binarySearch(periods, new Period(startDate, startDate, startDate, startDate));

		if(periodIndexEndDate < 0) {
			periodIndexEndDate		= -periodIndexEndDate-1;
		}
		if(periodIndexStartDate < 0) {
			periodIndexStartDate	= -periodIndexStartDate-1;
		} else {
			periodIndexStartDate	= periodIndexStartDate+1;
		}

		final Period startDatePeriod = periods.get(periodIndexStartDate);
		final Period endDatePeriod = periods.get(periodIndexEndDate);

		final double periodFraction =
				getDaycount(startDate, startDatePeriod.getPeriodEnd()) / getDaycount(startDatePeriod.getPeriodStart(), startDatePeriod.getPeriodEnd())
				+
				getDaycount(endDatePeriod.getPeriodStart(), endDate) / getDaycount(endDatePeriod.getPeriodStart(), endDatePeriod.getPeriodEnd())
				+
				(periodIndexEndDate - periodIndexStartDate) - 1;

		return periodFraction / frequency;
	}
}
