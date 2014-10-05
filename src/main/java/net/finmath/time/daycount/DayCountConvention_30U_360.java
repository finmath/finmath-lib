/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;

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
 * 		The method {@link #getDaycount(Calendar, Calendar) getDaycount} corresponds to the implementation of the "US method" of Excel function DAYS360, i.e., DAYS360(startDate,endDate,FALSE).
 * 	</li>
 * 	<li>
 * 		The method {@link #getDaycountFraction(Calendar, Calendar) getDaycountFraction} corresponds to the implementation of the "30U/360 method" of Excel function YEARFRAC, i.e., YEARFRAC(startDate,endDate,0).
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
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public double getDaycount(Calendar startDate, Calendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		double startDateDay 	= startDate.get(Calendar.DAY_OF_MONTH);
		double startDateMonth 	= startDate.get(Calendar.MONTH);
		double startDateYear 	= startDate.get(Calendar.YEAR);

		double endDateDay 		= endDate.get(Calendar.DAY_OF_MONTH);
		double endDateMonth 	= endDate.get(Calendar.MONTH);
		double endDateYear 		= endDate.get(Calendar.YEAR);

		if(
				isEndOfMonth &&
				startDate.get(Calendar.MONTH) == Calendar.FEBRUARY &&
				startDate.get(Calendar.DAY_OF_MONTH) == startDate.getActualMaximum(Calendar.DAY_OF_MONTH) &&
				endDate.get(Calendar.MONTH) == Calendar.FEBRUARY &&
				endDate.get(Calendar.DAY_OF_MONTH) == endDate.getActualMaximum(Calendar.DAY_OF_MONTH)
			) endDateDay = 30;

		if(
				isEndOfMonth &&
				startDate.get(Calendar.MONTH) == Calendar.FEBRUARY &&
				startDate.get(Calendar.DAY_OF_MONTH) == startDate.getActualMaximum(Calendar.DAY_OF_MONTH)
			) startDateDay = 30;
			
		if(endDateDay > 30 && startDateDay >= 30) endDateDay = 30;
		startDateDay = Math.min(startDateDay,30);
		
		return (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (endDateDay - startDateDay);
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		return getDaycount(startDate, endDate) / 360.0;
	}
}
