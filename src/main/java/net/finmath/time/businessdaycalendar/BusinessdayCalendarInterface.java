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

	/**
	 * Get an adjusted date for a given date and offset code.
	 * 
	 * First we create a new date by "adding" an offset to the base date.
	 * The offset may be given by codes like 1D, 2D, 1W, 2W, 1M, 2M, 3M,
	 * 1Y, 2Y, etc., where the letters denote the units as follows: D denotes days, W denotes weeks, M denotes month
	 * Y denotes years.
	 * 
	 * Next the result is adjusted according to the given dateRollConvention.
	 * 
	 * @param baseDate The start date.
	 * @param dateOffsetCode String containing date offset codes (like 2D, 1W, 3M, etc.) or combination of them separated by spaces.
	 * @param dateRollConvention The date roll convention to be used for the adjustment.
	 * @return The adjusted date applying dateRollConvention to the given date.
	 */
	Calendar getAdjustedDate(Calendar baseDate, String dateOffsetCode, DateRollConvention dateRollConvention);
}
