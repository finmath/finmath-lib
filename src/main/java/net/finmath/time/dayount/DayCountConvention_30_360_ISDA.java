/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.dayount;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * Calculated the day count as
 * <code>
 *		(endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay, startDateDay > 29 ? 30.0 : 31.0) - Math.min(startDateDay, 30.0))
 * </code>
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30_360_ISDA implements DayCountConventionInterface {

	/**
	 * Create a 30/360 ISDA daycount convention.
	 */
	public DayCountConvention_30_360_ISDA() {
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.dayount.DayCountConventionInterface#getDaycount(java.util.Calendar, java.util.Calendar)
	 */
	@Override
	public double getDaycount(GregorianCalendar startDate, GregorianCalendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		double startDateDay 	= startDate.get(Calendar.DAY_OF_MONTH);
		double startDateMonth 	= startDate.get(Calendar.MONTH);
		double startDateYear 	= startDate.get(Calendar.YEAR);
		
		double endDateDay 		= endDate.get(Calendar.DAY_OF_MONTH);
		double endDateMonth 	= endDate.get(Calendar.MONTH);
		double endDateYear 		= endDate.get(Calendar.YEAR);
		
		return (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay, startDateDay > 29 ? 30.0 : 31.0) - Math.min(startDateDay, 30.0));
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.dayount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(GregorianCalendar startDate, GregorianCalendar endDate) {
		if(startDate.after(endDate)) return -getDaycount(endDate,startDate);

		double startDateYear 	= startDate.get(Calendar.YEAR);
		double endDateYear 		= endDate.get(Calendar.YEAR);
		
		return getDaycount(startDate, endDate) / ((endDateYear - startDateYear + 1) * 360.0);
	}
}
