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

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_30E_360;
import net.finmath.time.daycount.DayCountConvention_30E_360_ISDA;
import net.finmath.time.daycount.DayCountConvention_ACT_360;
import net.finmath.time.daycount.DayCountConvention_ACT_365;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;

/**
 * @author Christian Fries
 *
 */
public class ScheduleGenerator {

	public enum Frequency {
		QUATERLY,
		SEMIANNUAL,
		ANNUAL
	}
	
	public enum DaycountConvention {
		E30_360_ISDA,
		E30_360,
		ACT_360,
		ACT_365,
		ACT_ACT_ISDA,
		ACT_ACT
	}
	
	public enum ShortPeriodConvention {
		FIRST,
		LAST
	}

	private ScheduleGenerator() {
	}

	/**
	 * Simple schedule generation.
	 * 
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods. Date rolling is ignored.
	 * 
	 * @param spotDate The start date of the first period.
	 * @param frequency The frequency.
	 * @param maturity The end date of the first period.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @return
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Date spotDate,
			String frequency,
			String maturity,
			String daycountConvention,
			String shortPeriodConvention
			)
	{
		Calendar spotDateAsCalendar = GregorianCalendar.getInstance();
		spotDateAsCalendar.setTime(spotDate);

		Calendar maturityAsCalendar = createMaturityFromCode(spotDateAsCalendar, maturity);

		return createScheduleFromConventions(
				spotDateAsCalendar,
				Frequency.valueOf(frequency.replace("/", "_").toUpperCase()),
				maturityAsCalendar, 
				DaycountConvention.valueOf(daycountConvention.replace("/", "_").toUpperCase()),
				ShortPeriodConvention.valueOf(shortPeriodConvention.replace("/", "_").toUpperCase())
				);
		
	}

	/**
	 * Simple schedule generation.
	 * 
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods. Date rolling is ignored.
	 * 
	 * @param spotDate The start date of the first period.
	 * @param frequency The frequency.
	 * @param maturity The end date of the first period.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @return
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Date spotDate,
			String frequency,
			double maturity,
			String daycountConvention,
			String shortPeriodConvention
			)
	{
		Calendar spotDateAsCalendar = GregorianCalendar.getInstance();
		spotDateAsCalendar.setTime(spotDate);

		Calendar maturityAsCalendar = createMaturityFromDouble(spotDateAsCalendar, maturity);

		return createScheduleFromConventions(
				spotDateAsCalendar,
				Frequency.valueOf(frequency.toUpperCase()),
				maturityAsCalendar, 
				DaycountConvention.valueOf(daycountConvention.replace("/", "_").toUpperCase()),
				ShortPeriodConvention.valueOf(shortPeriodConvention.toUpperCase())
				);
		
	}
	

	/**
	 * Simple schedule generation.
	 * 
	 * Generates a schedule based on some meta data. The schedule generation
	 * considers short periods. Date rolling is ignored.
	 * 
	 * @param spotDate The start date of the first period.
	 * @param frequency The frequency.
	 * @param maturity The end date of the first period.
	 * @param daycountConvention The daycount convention.
	 * @param shortPeriodConvention If short period exists, have it first or last.
	 * @return
	 */
	public static ScheduleInterface createScheduleFromConventions(
			Calendar spotDate,
			Frequency frequency,
			Calendar maturity,
			DaycountConvention daycountConvention,
			ShortPeriodConvention shortPeriodConvention
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

		int periodLengthInMonth = 12;
		switch(frequency) {
		case QUATERLY:
			periodLengthInMonth = 3;
			break;
		case SEMIANNUAL:
			periodLengthInMonth = 6;
			break;
		case ANNUAL:
		default:
			periodLengthInMonth = 12;
			break;
		}
		
		if(shortPeriodConvention == ShortPeriodConvention.LAST) {
			Calendar periodStartDate	= (Calendar)spotDate.clone();
			while(periodStartDate.before(maturity)) {
				Calendar periodEndDate		= (Calendar)periodStartDate.clone();		
				periodEndDate.add(Calendar.MONTH, periodLengthInMonth);

				if(periodEndDate.after(maturity)) {
					// Add last period, then terminate
					periods.add(new Period(periodStartDate, maturity, periodStartDate, maturity));
					break;
				}
				else {
					periods.add(new Period(periodStartDate, periodEndDate, periodStartDate, periodEndDate));
				}
				
				periodStartDate = (GregorianCalendar) periodEndDate.clone();
			}
		} else {
			Calendar periodEndDate	= (Calendar)maturity.clone();
			while(spotDate.before(periodEndDate)) {
				GregorianCalendar periodStartDate		= (GregorianCalendar)periodEndDate.clone();		
				periodStartDate.add(Calendar.MONTH, -periodLengthInMonth);

				if(periodEndDate.before(spotDate)) {
					// Add first period, then terminate
					periods.add(new Period(spotDate, periodEndDate, spotDate, periodEndDate));
					break;
				}
				else {
					periods.add(new Period(periodStartDate, periodEndDate, periodStartDate, periodEndDate));
				}
				
				periodEndDate = (Calendar) periodStartDate.clone();
			}
		}
		
		return new Schedule(spotDate, periods, daycountConventionObject);
	}
	
	
	/**
	 * Create a new date by "adding" a year fraction to the spot date.
	 * The year fraction may be given by codes like 1D, 2D, 1W, 2W, 1M, 2M, 3M,
	 * 1Y, 2Y, etc., where the letters denote the units as follows: D denotes days, W denotes weeks, M denotes month
	 * Y denotes years.
	 * 
	 * The function may be used to ease the creation of maturities in spreadsheets.
	 * 
	 * @param spotDate The start date.
	 * @param maturityCode The year fraction to be used for adding to the start date.
	 * @return A date corresponding the maturity.
	 */
	public static Calendar createMaturityFromCode(Calendar spotDate, String maturityCode) {
		maturityCode = maturityCode.trim();
		if(maturityCode.length() < 2) return null;

		char unitChar		= maturityCode.toLowerCase().charAt(maturityCode.length()-1);
		int maturityValue	= Integer.valueOf(maturityCode.substring(0, maturityCode.length()-1));
		
		int unit = 0;
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
		default:
			unit = Calendar.YEAR;
			break;
		}

		Calendar maturity = (Calendar)spotDate.clone();
		maturity.add(unit, maturityValue);
		return maturity;
	}

	/**
	 * Create a new date by "adding" a year fraction to the spot date.
	 * The year fraction is interpreted in a 30/360 way. More specifically,
	 * every integer unit advances by a year, each remaining fraction of 12
	 * advances by a month and each remaining fraction of 30 advances a day.
	 * 
	 * The function may be used to ease the creation of maturities in spreadsheets.
	 * 
	 * @param spotDateAsCalendar The start date.
	 * @param maturityDouble The year fraction to be used for adding to the start date.
	 * @return A date corresponding the maturity.
	 */
	private static Calendar createMaturityFromDouble(Calendar spotDateAsCalendar, double maturityDouble) {

		// Years
		Calendar maturity = (Calendar)spotDateAsCalendar.clone();
		maturity.add(Calendar.YEAR, (int)maturityDouble);
		
		// Months
		maturityDouble = (maturityDouble - (int)maturityDouble) * 12;
		maturity.add(Calendar.MONTH, (int)maturityDouble);
		
		// Days
		maturityDouble = (maturityDouble - (int)maturityDouble) * 30;
		maturity.add(Calendar.DAY_OF_YEAR, (int)maturityDouble);
		
		return maturity;
	}
}
