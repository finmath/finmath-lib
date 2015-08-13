/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.util.Calendar;

/**
 * Implementation of 30E/360 ISDA.
 * 
 * Calculates the day count as
 * <code>
 * 		(endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (Math.min(endDateDay,30) - Math.min(startDateDay, 30.0));
 * </code>
 * where for endDateDay is the to 30, if endDate is not the termination date and the last day of February.

 * The constructor offers a boolean to decide if endDate is treated as a termination date or not.
 * A termination date is the endDate of the last period in a schedule.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30E_360_ISDA implements DayCountConventionInterface {

	private final boolean isTreatEndDateAsTerminationDate;
	
	/**
	 * Create a 30E/360 ISDA daycount convention.
	 * 
	 * @param isTreatEndDateAsTerminationDate If true, the end date is treated as a termination date and that case the last day of february is <i>not</i> treated as 30.
	 */
	public DayCountConvention_30E_360_ISDA(boolean isTreatEndDateAsTerminationDate) {
		this.isTreatEndDateAsTerminationDate = isTreatEndDateAsTerminationDate;
	}

	/**
	 * Create a 30E/360 ISDA daycount convention using <code>isTreatEndDateAsTerminationDate = false</code>.
	 */
	public DayCountConvention_30E_360_ISDA() {
		this(false);
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


		// Check if we have last day of February
		boolean isStartDateLastDayOfFebruary = (startDateMonth == 1 && startDateDay == startDate.getActualMaximum(Calendar.DAY_OF_MONTH));
		boolean isEndDateLastDayOfFebruary = (endDateMonth == 1 && endDateDay == endDate.getActualMaximum(Calendar.DAY_OF_MONTH));

		// Last day of February and 31st of a month are both treated as "30".
		if(isStartDateLastDayOfFebruary || startDateDay == 31) startDateDay = 30;
		if((isEndDateLastDayOfFebruary && !isTreatEndDateAsTerminationDate) || endDateDay == 31) endDateDay = 30;

		return (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (endDateDay - Math.min(startDateDay, 30.0));
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.util.GregorianCalendar, java.util.GregorianCalendar)
	 */
	@Override
	public double getDaycountFraction(Calendar startDate, Calendar endDate) {
		return getDaycount(startDate, endDate) / 360.0;
	}
}
