/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.io.Serializable;
import java.time.LocalDate;

import org.apache.commons.lang3.Validate;

/**
 * @author Christian Fries
 * @version 1.0
 */
public interface BusinessdayCalendar extends Serializable {

	enum DateOffsetUnit {
		DAYS,
		BUSINESS_DAYS,
		WEEKS,
		MONTHS,
		YEARS;

		/**
		 * Get the date offset unit enum for a string (using common synonyms like "d", "b", "bd", "w").
		 *
		 * @param string The date roll convention name.
		 * @return The date roll convention enum.
		 */
		public static DateOffsetUnit getEnum(final String string) {
			Validate.notNull(string, "Date offset unit string must not be null.");

			if(string.equalsIgnoreCase("d")) {
				return DAYS;
			}
			if(string.equalsIgnoreCase("b")) {
				return BUSINESS_DAYS;
			}
			if(string.equalsIgnoreCase("bd")) {
				return BUSINESS_DAYS;
			}
			if(string.equalsIgnoreCase("w")) {
				return WEEKS;
			}
			if(string.equalsIgnoreCase("m")) {
				return MONTHS;
			}
			if(string.equalsIgnoreCase("y")) {
				return YEARS;
			}

			return DateOffsetUnit.valueOf(string.toUpperCase());
		}
	}

	enum DateRollConvention {
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
		public static DateRollConvention getEnum(final String string) {
			Validate.notNull(string, "Date roll convention string must not be null.");

			if(string.equalsIgnoreCase("actual")) {
				return UNADJUSTED;
			}
			if(string.equalsIgnoreCase("modfollow")) {
				return MODIFIED_FOLLOWING;
			}
			if(string.equalsIgnoreCase("modpreced")) {
				return MODIFIED_PRECEDING;
			}
			if(string.equalsIgnoreCase("follow")) {
				return 	FOLLOWING;
			}
			if(string.equalsIgnoreCase("preced")) {
				return 	PRECEDING;
			}

			return DateRollConvention.valueOf(string.toUpperCase());
		}
	}


	/**
	 * Test if a given date is a businessday.
	 *
	 * @param date The given date.
	 * @return True, if the given date is a businessday, otherwise false.
	 */
	boolean isBusinessday(LocalDate date);

	/**
	 * Get an adjusted date for a given date.
	 *
	 * @param date Given date to be adjusted.
	 * @param dateRollConvention The date roll convention to be used for the adjustment.
	 * @return The adjusted date applying dateRollConvention to the given date.
	 */
	LocalDate getAdjustedDate(LocalDate date, DateRollConvention dateRollConvention);

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
	LocalDate getAdjustedDate(LocalDate baseDate, String dateOffsetCode, DateRollConvention dateRollConvention);

	/**
	 * Find a new date by adding the given number of business days to a given base date.
	 *
	 * If baseDate is not a business date and businessDays is zero, the method returns the next business day.
	 *
	 * @param baseDate The starting date.
	 * @param businessDays The number of business days from the starting date (negative values are allowed).
	 * @return A date of a business day such that the number of business days between this one (including) and the start date (excluding) is businessDays.
	 */
	LocalDate getRolledDate(LocalDate baseDate, int businessDays);

	/**
	 * Create a new date by "adding" a year fraction to a given base date.
	 *
	 * <p>
	 * The date offset may be given by codes like 1D, 2D, 1W, 2W, 1M, 2M, 3M,
	 * 1Y, 2Y, etc., where the letters denote the units of the corresponding offset.
	 *
	 * If the date offset does not carry a letter code at the end, it will
	 * be interpreted as ACT/365 year fraction.
	 * </p>
	 *
	 * Date offsets can be given as (mapping to the corresponding <code>DateOffsetUnit</code>):
	 * <dl>
	 * 	<dt>days</dt>				<dd>"D", "DAYS"</dd>
	 * 	<dt>business days</dt>		<dd>"B", "BD", "BUSINESS_DAYS"</dd>
	 * 	<dt>weeks</dt>				<dd>"W", "WEEKS"</dd>
	 * 	<dt>months</dt>				<dd>"M", "MONTHS"</dd>
	 * 	<dt>years</dt>				<dd>"Y", "YEARS"</dd>
	 * </dl>
	 *
	 * <p>
	 * The function may be used to ease the creation of maturities in spreadsheets.
	 * </p>
	 *
	 * @param baseDate The start date.
	 * @param dateOffsetCode String containing date offset codes (like 2D, 1W, 3M, etc.) or combination of them separated by spaces.
	 * @return A date corresponding the date adding the offset to the start date.
	 */
	LocalDate getDateFromDateAndOffsetCode(LocalDate baseDate, String dateOffsetCode);
}
