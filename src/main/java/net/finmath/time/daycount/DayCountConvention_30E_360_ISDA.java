/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.time.LocalDate;
import java.time.Month;

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
 * @version 1.0
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
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycount(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycount(LocalDate startDate, LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			return -getDaycount(endDate,startDate);
		}

		int startDateDay 	= startDate.getDayOfMonth();
		int startDateMonth 	= startDate.getMonthValue();
		int startDateYear 	= startDate.getYear();

		int endDateDay 		= endDate.getDayOfMonth();
		int endDateMonth 	= endDate.getMonthValue();
		int endDateYear 	= endDate.getYear();


		// Check if we have last day of February
		boolean isStartDateLastDayOfFebruary = (startDateMonth == Month.FEBRUARY.getValue() && startDateDay == startDate.lengthOfMonth());
		boolean isEndDateLastDayOfFebruary = (endDateMonth == Month.FEBRUARY.getValue() && endDateDay == endDate.lengthOfMonth());

		// Last day of February and 31st of a month are both treated as "30".
		if(isStartDateLastDayOfFebruary || startDateDay == 31) {
			startDateDay = 30;
		}
		if((isEndDateLastDayOfFebruary && !isTreatEndDateAsTerminationDate) || endDateDay == 31) {
			endDateDay = 30;
		}

		return (endDateYear - startDateYear) * 360.0 + (endDateMonth - startDateMonth) * 30.0 + (endDateDay - Math.min(startDateDay, 30.0));
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConventionInterface#getDaycountFraction(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycountFraction(LocalDate startDate, LocalDate endDate) {
		return getDaycount(startDate, endDate) / 360.0;
	}
}
