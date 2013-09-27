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

		/**
		 * Get the date roll convention enum for a string (using common synonyms like "modfollow".
		 * 
		 * @param string The date roll convention name.
		 * @return The date roll convention enum.
		 */
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

	
	/**
	 * Test if a given date is a businessday.
	 * 
	 * @param date The given date.
	 * @return True, if the given date is a businessday, otherwise false.
	 */
	boolean isBusinessday(Calendar date);
	
	/**
	 * Get an adjusted date for a given date.
	 * 
	 * @param date Given date to be adjusted.
	 * @param dateRollConvention The date roll convention to be used for the adjustment.
	 * @return The adjusted date applying dateRollConvention to the given date.
	 */
	Calendar getAdjustedDate(Calendar date, DateRollConvention dateRollConvention);
}
