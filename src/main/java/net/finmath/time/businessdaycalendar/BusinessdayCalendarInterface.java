/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.util.Calendar;

/**
 * @author Christian Fries
 */
public interface BusinessdayCalendarInterface {
	
	public enum DateRollConvention {
		ACTUAL,
		FOLLOWING,
		PREVIOUS,
		MODIFIED_FOLLOWING,
		MODIFIED_PREVIOUS
	}

	boolean isBusinessday(Calendar date);
	
	/**
	 * @param date
	 * @param dateRollConvention
	 * @return
	 */
	Calendar getAdjustedDate(Calendar date, DateRollConvention dateRollConvention);
}
