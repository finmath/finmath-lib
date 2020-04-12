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
 * For the supported date roll conventions see {@link BusinessdayCalendar.DateRollConvention}.
 *
 * @author Christian Fries
 * @version 1.0
 */
public abstract class AbstractBusinessdayCalendar implements BusinessdayCalendar {

	private static final long serialVersionUID = -970677373015282512L;

	/* (non-Javadoc)
	 * @see net.finmath.time.BusinessdayCalendarInterface#getAdjustedDate(java.time.LocalDate, net.finmath.time.BusinessdayCalendarInterface.DateRollConvention)
	 */
	@Override
	public LocalDate getAdjustedDate(final LocalDate date, final DateRollConvention dateRollConvention) {
		if(dateRollConvention == DateRollConvention.UNADJUSTED) {
			return date;
		}
		else if(dateRollConvention == DateRollConvention.MODIFIED_FOLLOWING) {
			final LocalDate adjustedDate = getAdjustedDate(date, DateRollConvention.FOLLOWING);
			if (adjustedDate.getMonth() != date.getMonth()){
				return getAdjustedDate(date, DateRollConvention.PRECEDING);
			} else {
				return adjustedDate;
			}
		}
		else if(dateRollConvention == DateRollConvention.MODIFIED_PRECEDING) {
			final LocalDate adjustedDate = getAdjustedDate(date, DateRollConvention.PRECEDING);
			if (adjustedDate.getMonth() != date.getMonth()) {
				return getAdjustedDate(date, DateRollConvention.FOLLOWING);
			} else {
				return adjustedDate;
			}
		}
		else if(dateRollConvention == DateRollConvention.FOLLOWING || dateRollConvention == DateRollConvention.PRECEDING) {
			final int adjustment = dateRollConvention == DateRollConvention.FOLLOWING ? 1 : -1;
			LocalDate adjustedDate = date;
			while(!isBusinessday(adjustedDate)) {
				adjustedDate = adjustedDate.plusDays(adjustment);
			}
			return adjustedDate;
		}
		else {
			throw new IllegalArgumentException("Unknown date roll convention.");
		}
	}

	@Override
	public LocalDate getRolledDate(final LocalDate baseDate, int businessDays) {
		LocalDate			rolledDate			= baseDate;
		final int					direction			= businessDays >= 0 ? 1: -1;
		final DateRollConvention	dateRollConvention	= direction > 0 ? DateRollConvention.FOLLOWING : DateRollConvention.PRECEDING;
		while(businessDays != 0) {
			rolledDate = rolledDate.plusDays(direction);
			rolledDate = getAdjustedDate(rolledDate, dateRollConvention);
			businessDays -= direction;
		}
		return rolledDate;
	}

	@Override
	public LocalDate getAdjustedDate(final LocalDate baseDate, final String dateOffsetCode, final DateRollConvention dateRollConvention) {
		return getAdjustedDate(getDateFromDateAndOffsetCode(baseDate, dateOffsetCode), dateRollConvention);
	}

	@Override
	public LocalDate getDateFromDateAndOffsetCode(final LocalDate baseDate, String dateOffsetCode) {
		dateOffsetCode = dateOffsetCode.trim();

		final StringTokenizer tokenizer = new StringTokenizer(dateOffsetCode);

		LocalDate maturityDate = baseDate;
		while(tokenizer.hasMoreTokens()) {
			final String maturityCodeSingle = tokenizer.nextToken();
			final String[] maturityCodeSingleParts = maturityCodeSingle.split("(?<=[0-9|\\.])(?=[A-Z|a-z])");

			/*
			 * If no unit is given, the number is interpreted as ACT/365.
			 * Otherwise we switch according to dateOffsetUnit.
			 */
			if(maturityCodeSingleParts.length == 1) {
				// Try to parse a double as ACT/365
				final double maturityValue	= Double.valueOf(maturityCodeSingle);
				maturityDate = maturityDate.plusDays((int)Math.round(maturityValue * 365));
			}
			else if(maturityCodeSingleParts.length == 2) {
				final int maturityValue = Integer.valueOf(maturityCodeSingleParts[0]);
				final DateOffsetUnit dateOffsetUnit = DateOffsetUnit.getEnum(maturityCodeSingleParts[1]);

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

	public LocalDate[] createDateFromDateAndOffsetCodes(final LocalDate baseDate, final String[] dateOffsetCodes) {
		final LocalDate[] maturityDates = new LocalDate[dateOffsetCodes.length];
		for(int i = 0; i < dateOffsetCodes.length; i++) {
			final String dateOffsetCode = dateOffsetCodes[i].trim();

			final StringTokenizer tokenizer = new StringTokenizer(dateOffsetCode);

			LocalDate maturityDate = baseDate;
			while(tokenizer.hasMoreTokens()) {
				final String maturityCodeSingle = tokenizer.nextToken();
				final String[] maturityCodeSingleParts = maturityCodeSingle.split("(?<=[0-9|\\.])(?=[A-Z|a-z])");

				/*
				 * If no unit is given, the number is interpreted as ACT/365.
				 * Otherwise we switch according to dateOffsetUnit.
				 */
				if(maturityCodeSingleParts.length == 1) {
					// Try to parse a double as ACT/365
					final double maturityValue	= Double.valueOf(maturityCodeSingle);
					maturityDate = maturityDate.plusDays((int)Math.round(maturityValue * 365));
				}
				else if(maturityCodeSingleParts.length == 2) {
					final int maturityValue = Integer.valueOf(maturityCodeSingleParts[0]);
					final DateOffsetUnit dateOffsetUnit = DateOffsetUnit.getEnum(maturityCodeSingleParts[1]);

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
			maturityDates[i] = maturityDate;
		}
		return maturityDates;
	}

	public double[] convertOffsetCodesToTimes(final String[] dateOffsetCodes) {
		final double[] times = new double[dateOffsetCodes.length];
		for(int i = 0; i < dateOffsetCodes.length; i++) {
			final String dateOffsetCode = dateOffsetCodes[i].trim();

			final StringTokenizer tokenizer = new StringTokenizer(dateOffsetCode);

			double time = 0;
			while(tokenizer.hasMoreTokens()) {
				final String maturityCodeSingle = tokenizer.nextToken();
				final String[] maturityCodeSingleParts = maturityCodeSingle.split("(?<=[0-9|\\.])(?=[A-Z|a-z])");

				/*
				 * If no unit is given, the number is interpreted as ACT/365.
				 * Otherwise we switch according to dateOffsetUnit.
				 */
				if(maturityCodeSingleParts.length == 1) {
					// Try to parse a double as ACT/365
					time = Double.valueOf(maturityCodeSingle);
				}
				else if(maturityCodeSingleParts.length == 2) {
					final double maturityValue = Double.valueOf(maturityCodeSingleParts[0]);
					final DateOffsetUnit dateOffsetUnit = DateOffsetUnit.getEnum(maturityCodeSingleParts[1]);

					switch(dateOffsetUnit) {
					case DAYS:
					{
						time =  maturityValue/365;
						break;
					}
					case WEEKS:
					{
						time =  maturityValue/52;
						break;
					}
					case MONTHS:
					{
						time =  maturityValue/12;
						break;
					}
					case YEARS:
					{
						time =  maturityValue;
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
			times[i] = time;
		}
		return times;

	}

	@Override
	public String toString() {
		return "AbstractBusinessdayCalendar";
	}
}
