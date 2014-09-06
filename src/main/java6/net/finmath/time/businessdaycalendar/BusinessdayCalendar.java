/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.StringTokenizer;

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
	 * @see net.finmath.time.BusinessdayCalendarInterface#getAdjustedDate(java.util.Calendar, net.finmath.time.BusinessdayCalendarInterface.DateRollConvention)
	 */
	@Override
	public Calendar getAdjustedDate(Calendar date, DateRollConvention dateRollConvention) {
		if(dateRollConvention == DateRollConvention.UNADJUSTED) {
			return (Calendar)date.clone();
		}
		else if(dateRollConvention == DateRollConvention.MODIFIED_FOLLOWING) {
			Calendar adjustedDate = getAdjustedDate(date, DateRollConvention.FOLLOWING);
			if(adjustedDate.get(Calendar.MONTH) != date.get(Calendar.MONTH)) {
				return getAdjustedDate(date, DateRollConvention.PRECEDING);
			}
			else return adjustedDate;
		}
		else if(dateRollConvention == DateRollConvention.MODIFIED_PRECEDING) {
			Calendar adjustedDate = getAdjustedDate(date, DateRollConvention.PRECEDING);
			if(adjustedDate.get(Calendar.MONTH) != date.get(Calendar.MONTH)) {
				return getAdjustedDate(date, DateRollConvention.FOLLOWING);
			}
			else return adjustedDate;
		}
		else if(dateRollConvention == DateRollConvention.FOLLOWING || dateRollConvention == DateRollConvention.PRECEDING) {
			int adjustment = dateRollConvention == DateRollConvention.FOLLOWING ? 1 : -1;
			Calendar adjustedDate = (Calendar)date.clone();
			while(!isBusinessday(adjustedDate)) {
				adjustedDate.add(Calendar.DAY_OF_YEAR, adjustment);
			}
			return adjustedDate;
		}

		throw new IllegalArgumentException("Unknown date roll convention.");
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface#getAdjustedDate(java.util.Calendar, java.lang.String, net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention)
	 */
	public Calendar getAdjustedDate(Calendar baseDate, String dateOffsetCode, DateRollConvention dateRollConvention) {
		return this.getAdjustedDate(createDateFromDateAndOffsetCode(baseDate, dateOffsetCode), dateRollConvention);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface#getRolledDate(java.util.Calendar, int)
	 */
	public Calendar getRolledDate(Calendar baseDate, int businessDays) {
		Calendar			rolledDate			= (Calendar)baseDate.clone();
		int					direction			= businessDays >= 0 ? 1: -1;
		DateRollConvention	dateRollConvention	= direction > 0 ? DateRollConvention.FOLLOWING : DateRollConvention.PRECEDING;
		while(businessDays != 0) {
			rolledDate.add(Calendar.DAY_OF_YEAR, direction);
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
	public Date getAdjustedDate(Date baseDate, String dateOffsetCode, DateRollConvention dateRollConvention) {
		Calendar baseDateAsCalendar = GregorianCalendar.getInstance();
		baseDateAsCalendar.setTime(baseDate);
		return this.getAdjustedDate(createDateFromDateAndOffsetCode(baseDateAsCalendar, dateOffsetCode), dateRollConvention).getTime();
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
	public static Calendar createDateFromDateAndOffsetCode(Calendar baseDate, String dateOffsetCode) {
		dateOffsetCode = dateOffsetCode.trim();

		StringTokenizer tokenizer = new StringTokenizer(dateOffsetCode);
		
		Calendar maturity = (Calendar)baseDate.clone();
		while(tokenizer.hasMoreTokens()) {
			String maturityCodeSingle = tokenizer.nextToken();

			char	unitChar		= maturityCodeSingle.toLowerCase().charAt(maturityCodeSingle.length()-1);
			int		unit;
			switch(unitChar) {
			case 'd':
				unit = Calendar.DAY_OF_YEAR;
				break;
			case 'w':
				unit = Calendar.WEEK_OF_YEAR;
				break;
			case 'm':
				unit = Calendar.MONTH;
				break;
			case 'y':
				unit = Calendar.YEAR;
				break;
			default:
				unit = Calendar.FIELD_COUNT;
			}

			if(unit != Calendar.FIELD_COUNT) {
				int maturityValue		= Integer.valueOf(maturityCodeSingle.substring(0, maturityCodeSingle.length()-1));
				maturity.add(unit, maturityValue);
			}
			else {
				// Try to parse a double as ACT/365
				double maturityValue	= Double.valueOf(maturityCodeSingle);
				maturity.add(Calendar.DAY_OF_YEAR, (int)Math.round(maturityValue * 365));
			}
		}

		return maturity;
	}
}
