/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;

/**
 * This is a special day count convention, where the day count between two dates is always 0.0
 * and the year fraction for an interval is always 1.0.
 * 
 * The latter property is probably the relevant property: you may use this day count convention if a coupon period
 * pays the coupon without applying a year fraction of the accrual period, but the implementation requires an object implementing
 * {@link DayCountConventionInterface}.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_NONE implements DayCountConventionInterface {

	/**
	 * Create a day count convention with a constant year fraction of 1.0 for all periods.
	 */
	public DayCountConvention_NONE() {
	}

	@Override
	public double getDaycount(Calendar startDate, Calendar endDate) {
		return 0.0;
	}

	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		return 1.0;
	}
}
