/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;

/**
 * Calculates the day count as
 * <code>
 *		(endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay, 30.0) - Math.min(startDateDay, 30.0))
 * </code>
 * 
 * <ul>
 * 	<li>
 * 		The method {@link #getDaycount(Calendar, Calendar) getDaycount} corresponds to the implementation of the "European method" of Excel function DAYS360, i.e., DAYS360(startDate,endDate,TRUE).
 * 	</li>
 * 	<li>
 * 		The method {@link #getDaycountFraction(Calendar, Calendar) getDaycountFraction} corresponds to the implementation of the "30E/360 method" of Excel function YEARFRAC, i.e., YEARFRAC(startDate,endDate,4).
 * 	</li>
 * </ul>
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30E_360 implements DayCountConventionInterface {

	/**
	 * Create a 30E/360 daycount convention.
	 */
	public DayCountConvention_30E_360() {
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

		return (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay, 30.0) - Math.min(startDateDay, 30.0));
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		return getDaycount(startDate, endDate) / 360.0;
	}
}
