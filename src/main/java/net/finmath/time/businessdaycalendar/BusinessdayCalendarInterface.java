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
		UNADJUSTED,
		FOLLOWING,
		PRECEDING,
		MODIFIED_FOLLOWING,
		MODIFIED_PRECEDING;

		public static DateRollConvention getEnum(String string) {
	        if(string == null) throw new IllegalArgumentException();
	        if(string.equalsIgnoreCase("actual"))		return UNADJUSTED;
	        if(string.equalsIgnoreCase("modfollow"))	return MODIFIED_FOLLOWING;
	        if(string.equalsIgnoreCase("modpreced"))	return MODIFIED_PRECEDING;
	        if(string.equalsIgnoreCase("follow"))		return 	FOLLOWING;
	        if(string.equalsIgnoreCase("preced"))		return 	PRECEDING;

	        return DateRollConvention.valueOf(string.toUpperCase());
	    }
	}

	boolean isBusinessday(Calendar date);
	
	/**
	 * @param date
	 * @param dateRollConvention
	 * @return
	 */
	Calendar getAdjustedDate(Calendar date, DateRollConvention dateRollConvention);
}
