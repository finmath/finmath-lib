/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.util.StringTokenizer;

import org.joda.time.LocalDate;
import org.joda.time.ReadablePeriod;

/**
 * Base class for all business day calendars.
 * 
 * Implements date rolling and date adjustment.
 * For the supported date roll conventions see {@link BusinessdayCalendarInterface.DateRollConvention}.
 * 
 * @author Christian Fries
 */
public abstract class BusinessdayCalendar implements BusinessdayCalendarInterface {

	/* (non-Javadoc)
	 * @see net.finmath.time.BusinessdayCalendarInterface#getAdjustedDate(java.time.LocalDate, net.finmath.time.BusinessdayCalendarInterface.DateRollConvention)
	 */
	@Override
	public LocalDate getAdjustedDate(LocalDate date, DateRollConvention dateRollConvention) {
		if(dateRollConvention == DateRollConvention.UNADJUSTED) {
			return date;
		}
		else if(dateRollConvention == DateRollConvention.MODIFIED_FOLLOWING) {
			LocalDate adjustedDate = getAdjustedDate(date, DateRollConvention.FOLLOWING);
			if (adjustedDate.getMonthOfYear() != date.getMonthOfYear()){
				return getAdjustedDate(date, DateRollConvention.PRECEDING);
			}
			else return adjustedDate;
		}
		else if(dateRollConvention == DateRollConvention.MODIFIED_PRECEDING) {
			LocalDate adjustedDate = getAdjustedDate(date, DateRollConvention.PRECEDING);
			if (adjustedDate.getMonthOfYear() != date.getMonthOfYear()) {
				return getAdjustedDate(date, DateRollConvention.FOLLOWING);
			}
			else return adjustedDate;
		}
		else if(dateRollConvention == DateRollConvention.FOLLOWING || dateRollConvention == DateRollConvention.PRECEDING) {
			int adjustment = dateRollConvention == DateRollConvention.FOLLOWING ? 1 : -1;
			LocalDate adjustedDate = date;
			while(!isBusinessday(adjustedDate)) {
				adjustedDate = adjustedDate.plusDays(adjustment);
			}
			return adjustedDate;
		}

		throw new IllegalArgumentException("Unknown date roll convention.");
	}
	
	
	/* (non-Javadoc)
	 * @see net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface#getRolledDate(java.time.LocalDate, int)
	 */
	public LocalDate getRolledDate(LocalDate baseDate, int businessDays) {
		LocalDate			rolledDate			= baseDate;
		int					direction			= businessDays >= 0 ? 1: -1;
		DateRollConvention	dateRollConvention	= direction > 0 ? DateRollConvention.FOLLOWING : DateRollConvention.PRECEDING;
		while(businessDays != 0) {
			rolledDate = rolledDate.plusDays(direction);
			rolledDate = getAdjustedDate(rolledDate, dateRollConvention);
			businessDays -= direction;
		}
		return rolledDate;
	}

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
	public LocalDate getAdjustedDate(LocalDate baseDate, String dateOffsetCode, DateRollConvention dateRollConvention) {
		return this.getAdjustedDate(createDateFromDateAndOffsetCode(baseDate, dateOffsetCode), dateRollConvention);
	}

	/**
	 * Create a new date by "adding" a year fraction to a given base date.
	 * 
	 * <p>
	 * The date offset may be given by codes like 1D, 2D, 1W, 2W, 1M, 2M, 3M,
	 * 1Y, 2Y, etc., where the letters denote the units as follows: D denotes days, W denotes weeks, M denotes month
	 * Y denotes years. If the date offset does not carry a letter code at the end, it will
	 * be interpreted as ACT/365 year fraction.
	 * </p>
	 * 
	 * <p>
	 * The function may be used to ease the creation of maturities in spreadsheets.
	 * </p>
	 * 
	 * @param baseDate The start date.
	 * @param dateOffsetCode String containing date offset codes (like 2D, 1W, 3M, etc.) or combination of them separated by spaces.
	 * @return A date corresponding the date adding the offset to the start date.
	 */
	public static LocalDate createDateFromDateAndOffsetCode(LocalDate baseDate, String dateOffsetCode) {
		dateOffsetCode = dateOffsetCode.trim();

		StringTokenizer tokenizer = new StringTokenizer(dateOffsetCode);
		
		LocalDate maturity = baseDate;
		while(tokenizer.hasMoreTokens()) {
			String maturityCodeSingle = tokenizer.nextToken();

			char unitChar = maturityCodeSingle.toLowerCase().charAt(maturityCodeSingle.length()-1);

			switch(unitChar) {
			case 'd':
			{
				int maturityValue = Integer.valueOf(maturityCodeSingle.substring(0, maturityCodeSingle.length()-1));
				maturity = maturity.plusDays(maturityValue);
				break;
			}
			case 'w':
			{
				int maturityValue = Integer.valueOf(maturityCodeSingle.substring(0, maturityCodeSingle.length()-1));
				maturity = maturity.plusWeeks(maturityValue);
				break;
			}
			case 'm':
			{
				int maturityValue = Integer.valueOf(maturityCodeSingle.substring(0, maturityCodeSingle.length()-1));
				maturity = maturity.plusMonths(maturityValue);
				break;
			}
			case 'y':
			{
				int maturityValue = Integer.valueOf(maturityCodeSingle.substring(0, maturityCodeSingle.length()-1));
				maturity = maturity.plusYears(maturityValue);
				break;
			}
			default:
				// Try to parse a double as ACT/365
				double maturityValue	= Double.valueOf(maturityCodeSingle);
				maturity = maturity.plusDays((int)Math.round(maturityValue * 365));
			}
		}

		return maturity;
	}
}
