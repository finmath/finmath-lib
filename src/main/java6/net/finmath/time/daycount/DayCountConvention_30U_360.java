/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;

/**
 * Calculates the day count using the US 30/360 adjusted method. The daycount is calculated via
 * <code>
 *		(endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (endDateDay - startDateDay)
 * </code>
 * where startDateDay and endDateDay are adjusted towards 30, when being larger than 30 or if isEndOfMonth, also if
 * they fall on the last day of February. See 30/360 US definition for details.
 * 
 * This day count convention is sometime called <i>Bond basis</i>.
 * 
 * <ul>
 * 	<li>
 * 		The method {@link #getDaycount(LocalDate, LocalDate) getDaycount} corresponds to the implementation of the "US method" of Excel function DAYS360, i.e., DAYS360(startDate,endDate,FALSE).
 * 	</li>
 * 	<li>
 * 		The method {@link #getDaycountFraction(LocalDate, LocalDate) getDaycountFraction} corresponds to the implementation of the "30U/360 method" of Excel function YEARFRAC, i.e., YEARFRAC(startDate,endDate,0).
 * 	</li>
 * </ul>
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30U_360 implements DayCountConventionInterface {

	private boolean isEndOfMonth =  true;
	
	/**
	 * Create a 30U/360 day count convention.
	 */
	public DayCountConvention_30U_360() {
	}

	/**
	 * Create a 30U/360 day count convention.
	 * 
	 * @param isEndOfMonth If true, an end-of-month day will always count as "30", even if we are in February.
	 */
	public DayCountConvention_30U_360(boolean isEndOfMonth) {
		this.isEndOfMonth = isEndOfMonth;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycount(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) return -getDaycount(endDate,startDate);

		int startDateDay 	= startDate.getDayOfMonth();
		int startDateMonth 	= startDate.dayOfMonth().getMaximumValue();
		int startDateYear 	= startDate.getYear();

		int endDateDay 		= endDate.getDayOfMonth();
		int endDateMonth 	= endDate.dayOfMonth().getMaximumValue();
		int endDateYear 	= endDate.getYear();

		if(
				isEndOfMonth &&
				startDate.getMonthOfYear() == DateTimeConstants.FEBRUARY &&
				startDate.getDayOfMonth() == startDate.dayOfMonth().getMaximumValue() && 
				endDate.getMonthOfYear() == DateTimeConstants.FEBRUARY &&
				endDate.getDayOfMonth() == endDate.dayOfMonth().getMaximumValue()
			) endDateDay = 30;

		if(
				isEndOfMonth &&
				startDate.getMonthOfYear() == DateTimeConstants.FEBRUARY &&
				startDate.getDayOfMonth() == startDate.dayOfMonth().getMaximumValue()
			) startDateDay = 30;
			
		if(endDateDay > 30 && startDateDay >= 30) endDateDay = 30;
		startDateDay = Math.min(startDateDay,30);
		
		return (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (endDateDay - startDateDay);
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycountFraction(LocalDate startDate, LocalDate endDate) {
		return getDaycount(startDate, endDate) / 360.0;
	}
}
