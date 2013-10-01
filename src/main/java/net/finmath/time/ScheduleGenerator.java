/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.finmath.time.businessdaycalendar.BusinessdayCalendarAny;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_30E_360;
import net.finmath.time.daycount.DayCountConvention_30E_360_ISDA;
import net.finmath.time.daycount.DayCountConvention_30U_360;
import net.finmath.time.daycount.DayCountConvention_ACT_360;
import net.finmath.time.daycount.DayCountConvention_ACT_365;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;

/**
 * Generates a schedule based on some meta data (frequency, maturity, date roll convention, etc.).
 * A schedule is just a collection of {@link net.finmath.time.Period}s.
 * 
 * <ul>
 * 	<li>The period length is specified via {@link net.finmath.time.ScheduleGenerator.Frequency}.
 * 	<li>The schedule generation considers short periods via the specification of {@link net.finmath.time.ScheduleGenerator.DaycountConvention}.</li>
 * 	<li>The schedule may use an externally provided business day adjustment via an object implementing {@link net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface}</li>
 * 	<li>You may specify fixing and payment adjustments.
 * </ul>
 * 
 * @author Christian Fries
 */
public class ScheduleGenerator {

	/**
	 * Possible frequencies supported by {@link ScheduleGenerator}.
	 * 
	 * @author Christian Fries
	 */
	public enum Frequency {
		/** Daily periods. **/
		DAILY,
		/** Weekly periods. **/
		WEEKLY,
		/** Three months periods. **/
		QUATERLY,
		/** Six months periods. **/
		SEMIANNUAL,
		/** Twelve months periods. **/
		ANNUAL,
		/** A single period, i.e., the period is as long as from start to maturity. **/
		TENOR
	}
	
	/**
	 * Possible day count conventions supported by {@link DaycountConvention}.
	 * 
	 * @author Christian Fries
	 */
	public enum DaycountConvention {
		/** See {@link net.finmath.time.daycount.DayCountConvention_30E_360_ISDA }. **/
		E30_360_ISDA,
		/** See {@link net.finmath.time.daycount.DayCountConvention_30E_360 }. **/
		E30_360,
		/** See {@link net.finmath.time.daycount.DayCountConvention_30U_360 }. **/
		U30_360,
		/** See {@link net.finmath.time.daycount.DayCountConvention_ACT_360 }. **/
		ACT_360,
		/** See {@link net.finmath.time.daycount.DayCountConvention_ACT_365 }. **/
		ACT_365,
		/** See {@link net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA }. **/
		ACT_ACT_ISDA,
		ACT_ACT;

		public static DaycountConvention getEnum(String string) {
	        if(string == null) throw new IllegalArgumentException();
	        if(string.equalsIgnoreCase("30e/360 isda"))	return E30_360_ISDA;
	        if(string.equalsIgnoreCase("e30/360 isda"))	return E30_360_ISDA;
	        if(string.equalsIgnoreCase("30e/360"))		return E30_360;
	        if(string.equalsIgnoreCase("e30/360"))		return E30_360;
	        if(string.equalsIgnoreCase("30u/360"))		return U30_360;
	        if(string.equalsIgnoreCase("u30/360"))		return U30_360;
	        if(string.equalsIgnoreCase("act/360"))		return ACT_360;
	        if(string.equalsIgnoreCase("act/365"))		return ACT_365;
	        if(string.equalsIgnoreCase("act/act isda"))	return ACT_ACT_ISDA;
	        if(string.equalsIgnoreCase("act/act"))		return ACT_ACT;

	        return DaycountConvention.valueOf(string.toUpperCase());
	    }
	}
	
	/**
	 * Possible stub period conventions supported by {@link DaycountConvention}.
	 * 
	 * @author Christian Fries
	 */
	public enum ShortPeriodConvention {
		/** The first period will be shorter, if a regular period does not fit. **/
		FIRST,
		/** The last period will be shorter, if a regular period does not fit. **/
		LAST
	}

	private ScheduleGenerator() {
	}

	/**
	 * Simple schedule generation.
	 * 
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods.
	 * 
	 * @param referenceDate The date which is used in the schedule to internally convert dates to doubles, i.e., the date where t=0.
	 * @param startDate The start date of the first period.
	 * @param maturity The end date of the first period.
	 * @param frequency The frequency.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @param dateRollConvention Adjustment to be applied to the all dates.
	 * @param businessdayCalendar Businessday calendar (holiday calendar) to be used for date roll adjustment.
	 * @param fixingOffsetDays Number of days to be added to period start to get the fixing date.
	 * @param paymentOffsetDays Number of days to be added to period end to get the payment date.
	 * @return The corresponding schedule
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Calendar referenceDate,
			Calendar startDate,
			Calendar maturity,
			Frequency frequency,
			DaycountConvention daycountConvention,
			ShortPeriodConvention shortPeriodConvention,
			DateRollConvention dateRollConvention,
			BusinessdayCalendarInterface businessdayCalendar,
			int	fixingOffsetDays,
			int	paymentOffsetDays
			)
	{
		/*
		 * Generate periods - note: we do not use any date roll convention
		 */
		ArrayList<Period> periods = new ArrayList<Period>();
	
		DayCountConventionInterface daycountConventionObject = null;
		switch (daycountConvention) {
		case E30_360_ISDA:
			daycountConventionObject = new DayCountConvention_30E_360_ISDA();
			break;
		case E30_360:
			daycountConventionObject = new DayCountConvention_30E_360();
			break;
		case U30_360:
			daycountConventionObject = new DayCountConvention_30U_360();
			break;
		case ACT_360:
			daycountConventionObject = new DayCountConvention_ACT_360();
			break;
		case ACT_365:
			daycountConventionObject = new DayCountConvention_ACT_365();
			break;
		case ACT_ACT_ISDA:
		case ACT_ACT:
		default:
			daycountConventionObject = new DayCountConvention_ACT_ACT_ISDA();
			break;
		}
	
		int periodLengthDays	= 0;
		int periodLengthWeeks	= 0;
		int periodLengthMonth	= 0;
		switch(frequency) {
		case DAILY:
			periodLengthDays	= 1;
			break;
		case WEEKLY:
			periodLengthDays	= 1;
			break;
		case QUATERLY:
			periodLengthMonth	= 3;
			break;
		case SEMIANNUAL:
			periodLengthMonth	= 6;
			break;
		case ANNUAL:
		default:
			periodLengthMonth	= 12;
			break;
		case TENOR:
			periodLengthMonth	= 100000;
			break;
		}
		
		if(shortPeriodConvention == ShortPeriodConvention.LAST) {
			/*
			 * Going forward on periodStartDate, starting with startDate as periodStartDate
			 */
			Calendar periodStartDateUnadjusted	= (Calendar)startDate.clone();
			Calendar periodStartDate			= businessdayCalendar.getAdjustedDate(periodStartDateUnadjusted, dateRollConvention);
			while(periodStartDateUnadjusted.before(maturity)) {
				// Determine period end
				Calendar periodEndDateUnadjusted		= (Calendar)periodStartDateUnadjusted.clone();		
				periodEndDateUnadjusted.add(Calendar.DAY_OF_YEAR, periodLengthDays);
				periodEndDateUnadjusted.add(Calendar.WEEK_OF_YEAR, periodLengthWeeks);
				periodEndDateUnadjusted.add(Calendar.MONTH, periodLengthMonth);
				if(periodEndDateUnadjusted.after(maturity)) periodEndDateUnadjusted = maturity;

				// Adjust period
				Calendar periodEndDate		= businessdayCalendar.getAdjustedDate(periodEndDateUnadjusted, dateRollConvention);
	
				// Adjust fixing date
				Calendar fixingDate = (Calendar)periodStartDate.clone();
				fixingDate.add(Calendar.DAY_OF_YEAR, fixingOffsetDays);
				fixingDate = businessdayCalendar.getAdjustedDate(fixingDate, dateRollConvention);

				// Adjust payment date
				Calendar paymentDate = (Calendar)periodEndDate.clone();
				paymentDate.add(Calendar.DAY_OF_YEAR, paymentOffsetDays);
				paymentDate = businessdayCalendar.getAdjustedDate(paymentDate, dateRollConvention);
	
				// Create period
				periods.add(new Period(fixingDate, paymentDate, periodStartDate, periodEndDate));
	
				periodStartDate				= (Calendar)periodEndDate.clone();
				periodStartDateUnadjusted	= (Calendar)periodEndDateUnadjusted.clone();
			}
		} else {
			/*
			 * Going backward on periodEndDate, starting with maturity as periodEndDate
			 */
			Calendar periodEndDateUnadjusted	= (Calendar)maturity.clone();
			Calendar periodEndDate				= businessdayCalendar.getAdjustedDate(periodEndDateUnadjusted, dateRollConvention);
			while(periodEndDateUnadjusted.after(startDate)) {
				// Determine period start
				Calendar periodStartDateUnadjusted		= (Calendar)periodEndDateUnadjusted.clone();
				periodStartDateUnadjusted.add(Calendar.DAY_OF_YEAR, -periodLengthDays);
				periodStartDateUnadjusted.add(Calendar.WEEK_OF_YEAR, -periodLengthWeeks);
				periodStartDateUnadjusted.add(Calendar.MONTH, -periodLengthMonth);
				if(periodStartDateUnadjusted.before(startDate)) periodStartDateUnadjusted = startDate;

				// Adjust period
				Calendar periodStartDate	= businessdayCalendar.getAdjustedDate(periodStartDateUnadjusted, dateRollConvention);
	
				// Adjust fixing date
				Calendar fixingDate = (Calendar)periodStartDate.clone();
				fixingDate.add(Calendar.DAY_OF_YEAR, fixingOffsetDays);
				fixingDate = businessdayCalendar.getAdjustedDate(fixingDate, dateRollConvention);
	
				// Adjust payment date
				Calendar paymentDate = (Calendar)periodEndDate.clone();
				paymentDate.add(Calendar.DAY_OF_YEAR, paymentOffsetDays);
				paymentDate = businessdayCalendar.getAdjustedDate(paymentDate, dateRollConvention);
	
				// Create period
				periods.add(0, new Period(fixingDate, paymentDate, periodStartDate, periodEndDate));
				
				periodEndDate			= (Calendar)periodStartDate.clone();
				periodEndDateUnadjusted	= (Calendar)periodStartDateUnadjusted.clone();
			}
		}
		
		return new Schedule(referenceDate, periods, daycountConventionObject);
	}

	/**
	 * Simple schedule generation.
	 * 
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods. Date rolling is ignored.
	 * 
	 * @param referenceDate The date which is used in the schedule to internally convert dates to doubles, i.e., the date where t=0.
	 * @param startDate The start date of the first period.
	 * @param maturityDate The end date of the last period.
	 * @param frequency The frequency.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @param dateRollConvention Adjustment to be applied to the all dates.
	 * @param businessdayCalendar Businessday calendar (holiday calendar) to be used for date roll adjustment.
	 * @param fixingOffsetDays Number of days to be added to period start to get the fixing date.
	 * @param paymentOffsetDays Number of days to be added to period end to get the payment date.
	 * @return The corresponding schedule
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Date referenceDate,
			Date startDate,
			Date maturityDate,
			String frequency,
			String daycountConvention,
			String shortPeriodConvention,
			String dateRollConvention,
			BusinessdayCalendarInterface businessdayCalendar,
			int	fixingOffsetDays,
			int	paymentOffsetDays
			)
	{
		Calendar referenceDateAsCalendar = GregorianCalendar.getInstance();
		referenceDateAsCalendar.setTime(referenceDate);

		Calendar startDateAsCalendar = GregorianCalendar.getInstance();
		startDateAsCalendar.setTime(startDate);

		Calendar maturityDateAsCalendar = GregorianCalendar.getInstance();
		maturityDateAsCalendar.setTime(maturityDate);

		return createScheduleFromConventions(
				referenceDateAsCalendar,
				startDateAsCalendar,
				maturityDateAsCalendar,
				Frequency.valueOf(frequency.replace("/", "_").toUpperCase()), 
				DaycountConvention.getEnum(daycountConvention),
				ShortPeriodConvention.valueOf(shortPeriodConvention.replace("/", "_").toUpperCase()),
				DateRollConvention.getEnum(dateRollConvention),
				businessdayCalendar,
				fixingOffsetDays,
				paymentOffsetDays
				);
	}

	/**
	 * Simple schedule generation.
	 * 
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods. Date rolling is ignored.
	 * 
	 * @param referenceDate The date which is used in the schedule to internally convert dates to doubles, i.e., the date where t=0.
	 * @param maturity The end date of the first period entered as a code like 1D, 1W, 1M, 2M, 3M, 1Y, etc.
	 * @param frequency The frequency.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @param dateRollConvention Adjustment to be applied to the all dates.
	 * @param businessdayCalendar Businessday calendar (holiday calendar) to be used for date roll adjustment.
	 * @param fixingOffsetDays Number of days to be added to period start to get the fixing date.
	 * @param paymentOffsetDays Number of days to be added to period end to get the payment date.
	 * @param startDate The start date of the first period.
	 * @return The corresponding schedule
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Date referenceDate,
			String startOffset,
			String maturity,
			String frequency,
			String daycountConvention,
			String shortPeriodConvention,
			String dateRollConvention,
			BusinessdayCalendarInterface businessdayCalendar,
			int	fixingOffsetDays,
			int	paymentOffsetDays
			)
	{
		Calendar referenceDateAsCalendar = GregorianCalendar.getInstance();
		referenceDateAsCalendar.setTime(referenceDate);
	
		Calendar startDateAsCalendar = businessdayCalendar.getAdjustedDate(referenceDateAsCalendar, startOffset, DateRollConvention.getEnum(dateRollConvention));
	
		Calendar maturityAsCalendar = businessdayCalendar.getAdjustedDate(startDateAsCalendar, maturity, DateRollConvention.getEnum(dateRollConvention));
	
		return createScheduleFromConventions(
				referenceDateAsCalendar,
				startDateAsCalendar,
				maturityAsCalendar,
				Frequency.valueOf(frequency.replace("/", "_").toUpperCase()), 
				DaycountConvention.getEnum(daycountConvention),
				ShortPeriodConvention.valueOf(shortPeriodConvention.replace("/", "_").toUpperCase()),
				DateRollConvention.getEnum(dateRollConvention),
				businessdayCalendar,
				fixingOffsetDays,
				paymentOffsetDays
				);
	}

	/**
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods.
	 * 
	 * @param referenceDate The date which is used in the schedule to internally convert dates to doubles, i.e., the date where t=0.
	 * @param startDate The start date of the first period.
	 * @param frequency The frequency.
	 * @param maturity The end date of the first period.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @param dateRollConvention Adjustment to be applied to the all dates.
	 * @param businessdayCalendar Businessday calendar (holiday calendar) to be used for date roll adjustment.
	 * @param fixingOffsetDays Number of days to be added to period start to get the fixing date.
	 * @param paymentOffsetDays Number of days to be added to period end to get the payment date.
	 * @return The corresponding schedule
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Date referenceDate,
			Date startDate,
			String frequency,
			double maturity,
			String daycountConvention,
			String shortPeriodConvention,
			String dateRollConvention,
			BusinessdayCalendarInterface businessdayCalendar,
			int	fixingOffsetDays,
			int	paymentOffsetDays
			)
	{
		Calendar referenceDateAsCalendar = GregorianCalendar.getInstance();
		referenceDateAsCalendar.setTime(referenceDate);

		Calendar startDateAsCalendar = GregorianCalendar.getInstance();
		startDateAsCalendar.setTime(startDate);

		Calendar maturityAsCalendar = createDateFromDateAndOffset(startDateAsCalendar, maturity);
	
		return createScheduleFromConventions(
				referenceDateAsCalendar,
				startDateAsCalendar,
				maturityAsCalendar,
				Frequency.valueOf(frequency.toUpperCase()), 
				DaycountConvention.getEnum(daycountConvention),
				ShortPeriodConvention.valueOf(shortPeriodConvention.toUpperCase()),
				DateRollConvention.getEnum(dateRollConvention),
				businessdayCalendar,
				fixingOffsetDays,
				paymentOffsetDays
				);
		
	}

	/**
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods. Date rolling is ignored.
	 * 
	 * @param referenceDate The date which is used in the schedule to internally convert dates to doubles, i.e., the date where t=0.
	 * @param startDate The start date of the first period.
	 * @param frequency The frequency.
	 * @param maturity The end date of the first period.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @return The corresponding schedule
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Date referenceDate,
			Date startDate,
			String frequency,
			double maturity,
			String daycountConvention,
			String shortPeriodConvention
			)
	{
		return createScheduleFromConventions(
				referenceDate,
				startDate,
				frequency,
				maturity,
				daycountConvention,
				shortPeriodConvention,
				"UNADJUSTED",
				new BusinessdayCalendarAny(),
				0, 0);
	}

	/**
	 * Create a new date by "adding" a year fraction to the start date.
	 * The year fraction is interpreted in a 30/360 way. More specifically,
	 * every integer unit advances by a year, each remaining fraction of 12
	 * advances by a month and each remaining fraction of 30 advances a day.
	 * 
	 * The function may be used to ease the creation of maturities in spreadsheets.
	 * 
	 * @param baseDate The start date.
	 * @param offsetYearFrac The year fraction in 30/360 to be used for adding to the start date.
	 * @return A date corresponding the maturity.
	 */
	private static Calendar createDateFromDateAndOffset(Calendar baseDate, double offsetYearFrac) {

		// Years
		Calendar maturity = (Calendar)baseDate.clone();
		maturity.add(Calendar.YEAR, (int)offsetYearFrac);
		
		// Months
		offsetYearFrac = (offsetYearFrac - (int)offsetYearFrac) * 12;
		maturity.add(Calendar.MONTH, (int)offsetYearFrac);
		
		// Days
		offsetYearFrac = (offsetYearFrac - (int)offsetYearFrac) * 30;
		maturity.add(Calendar.DAY_OF_YEAR, (int)Math.round(offsetYearFrac));
		
		return maturity;
	}
}
