/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.util.StringTokenizer;

import org.joda.time.LocalDate;

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

	/* (non-Javadoc)
	 * @see net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface#getAdjustedDate(LocalDate, String, DateRollConvention)
	 */
	public LocalDate getAdjustedDate(LocalDate baseDate, String dateOffsetCode, DateRollConvention dateRollConvention) {
		return getAdjustedDate(createDateFromDateAndOffsetCode(baseDate, dateOffsetCode), dateRollConvention);
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface#createDateFromDateAndOffsetCode(LocalDate, createDateFromDateAndOffsetCode)
	 */
	public LocalDate createDateFromDateAndOffsetCode(LocalDate baseDate, String dateOffsetCode) {
		dateOffsetCode = dateOffsetCode.trim();

		StringTokenizer tokenizer = new StringTokenizer(dateOffsetCode);
		
		LocalDate maturityDate = baseDate;
		while(tokenizer.hasMoreTokens()) {
			String maturityCodeSingle = tokenizer.nextToken();
			// get unit identifier (usually last char but may be last two chars)
			char unitChar;
			int maturityValue;
			if(maturityCodeSingle.toLowerCase().substring(maturityCodeSingle.length()-2).equals("bd")) {
				unitChar = 'b';
				maturityValue = Integer.valueOf(maturityCodeSingle.substring(0, maturityCodeSingle.length()-2));
			} else {
				unitChar = maturityCodeSingle.toLowerCase().charAt(maturityCodeSingle.length()-1);
				maturityValue = Integer.valueOf(maturityCodeSingle.substring(0, maturityCodeSingle.length()-1));
			}
			
			// note that switch(string) would only work for java >=6
			switch(unitChar) {
			case 'd':
			{
				maturityDate = maturityDate.plusDays(maturityValue);
				break;
			}
			case 'b':
			{
				maturityDate = getRolledDate(maturityDate,maturityValue);
				break;
			}
			case 'w':
			{
				maturityDate = maturityDate.plusWeeks(maturityValue);
				break;
			}
			case 'm':
			{
				maturityDate = maturityDate.plusMonths(maturityValue);
				break;
			}
			case 'y':
			{
				maturityDate = maturityDate.plusYears(maturityValue);
				break;
			}
			default:
				throw new IllegalArgumentException("Cannot handle dateOffsetCode '" + dateOffsetCode + "'.");
				/**
				// Try to parse a double as ACT/365
				double maturityValue	= Double.valueOf(maturityCodeSingle);
				maturity = maturity.plusDays((int)Math.round(maturityValue * 365));
				*/
			}
		}

		return maturityDate;
	}
	
	public String toString() {
		return "BusinessdayCalendar";
	}
}
