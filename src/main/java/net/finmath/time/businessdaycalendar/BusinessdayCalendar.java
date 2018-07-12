/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.time.LocalDate;
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

	private static final long serialVersionUID = -970677373015282512L;

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
			if (adjustedDate.getMonth() != date.getMonth()){
				return getAdjustedDate(date, DateRollConvention.PRECEDING);
			} else {
				return adjustedDate;
			}
		}
		else if(dateRollConvention == DateRollConvention.MODIFIED_PRECEDING) {
			LocalDate adjustedDate = getAdjustedDate(date, DateRollConvention.PRECEDING);
			if (adjustedDate.getMonth() != date.getMonth()) {
				return getAdjustedDate(date, DateRollConvention.FOLLOWING);
			} else {
				return adjustedDate;
			}
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
			String[] maturityCodeSingleParts = maturityCodeSingle.split("(?<=[0-9|\\.])(?=[A-Z|a-z])");

			/*
			 * If no unit is given, the number is interpreted as ACT/365.
			 * Otherwise we switch according to dateOffsetUnit.
			 */
			if(maturityCodeSingleParts.length == 1) {
				// Try to parse a double as ACT/365
				double maturityValue	= Double.valueOf(maturityCodeSingle);
				maturityDate = maturityDate.plusDays((int)Math.round(maturityValue * 365));
			}
			else if(maturityCodeSingleParts.length == 2) {
				int maturityValue = Integer.valueOf(maturityCodeSingleParts[0]);
				DateOffsetUnit dateOffsetUnit = DateOffsetUnit.getEnum(maturityCodeSingleParts[1]);

				switch(dateOffsetUnit) {
				case DAYS:
				{
					maturityDate = maturityDate.plusDays(maturityValue);
					break;
				}
				case BUSINESS_DAYS:
				{
					maturityDate = getRolledDate(maturityDate,maturityValue);
					break;
				}
				case WEEKS:
				{
					maturityDate = maturityDate.plusWeeks(maturityValue);
					break;
				}
				case MONTHS:
				{
					maturityDate = maturityDate.plusMonths(maturityValue);
					break;
				}
				case YEARS:
				{
					maturityDate = maturityDate.plusYears(maturityValue);
					break;
				}
				default:
					throw new IllegalArgumentException("Cannot handle dateOffsetCode '" + dateOffsetCode + "'.");
				}
			}
			else {
				throw new IllegalArgumentException("Cannot handle dateOffsetCode '" + dateOffsetCode + "'.");
			}
		}

		return maturityDate;
	}

	public String toString() {
		return "BusinessdayCalendar";
	}
}
