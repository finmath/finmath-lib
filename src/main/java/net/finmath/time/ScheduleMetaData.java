package net.finmath.time;

import java.io.Serializable;
import java.time.LocalDate;

import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;

/**
 * Class to store any relevant information to generate schedules, which have different period structure but otherwise follow the same conventions.
 *
 * @author Christian Fries
 * @author Roland Bachl
 *
 */
public class ScheduleMetaData implements Serializable {

	private static final long serialVersionUID = 1547837440284116534L;

	/**
	 * Possible offset units to be used for schedule generation.
	 *
	 * @author Christian Fries
	 * @author Roland Bachl
	 *
	 */
	enum OffsetUnit {
		inMONTHS,
		inYEARS,
		inDAYS,
		inWEEKS
	}


	/**
	 * Determines the offset code of a forward contract from a schedule. Rounds the average period length to full months.
	 *
	 * @param schedule
	 * @return The offset code as String
	 */
	public static String getOffsetCodeFromSchedule(ScheduleInterface schedule) {

		double doubleLength = 0;
		for(int i = 0; i < schedule.getNumberOfPeriods(); i ++) {
			doubleLength += schedule.getPeriodLength(i);
		}
		doubleLength /= schedule.getNumberOfPeriods();

		doubleLength *= 12;
		int periodLength = (int) Math.round(doubleLength);


		String offsetCode = periodLength + "M";
		return offsetCode;
	}

	/**
	 * Determines the offset code of a forward contract from the name of a forward curve.
	 * This method will extract a group of one or more digits together with the first letter behind them, if any.
	 * If there are multiple groups of digits in the name, this method will extract the last.
	 * If there is no number in the string, this method will return null.
	 *
	 * @param curveName
	 * @return The offset code as String
	 */
	public static String getOffsetCodeFromCurveName(String curveName) {
		if(curveName == null || curveName.length() == 0) {
			return null;
		}
		String[] splits = curveName.split("(?<=\\D)(?=\\d)");
		String offsetCode = splits[splits.length-1];
		if(!Character.isDigit(offsetCode.charAt(0))) {
			return null;
		}

		offsetCode = offsetCode.split("(?<=[A-Za-z])(?=.)", 2)[0];
		offsetCode = offsetCode.replaceAll( "[\\W_]", "" );
		return offsetCode;
	}





	public final LocalDate referenceDate;
	public final Frequency frequency;
	public final DaycountConvention daycountConvention;
	public final ShortPeriodConvention shortPeriodConvention;
	public final DateRollConvention dateRollConvention;
	public final BusinessdayCalendarInterface businessdayCalendar;
	public final int	fixingOffsetDays;
	public final int	paymentOffsetDays;
	public final boolean isUseEndOfMonth;


	/**
	 * Construct the ScheduleMetaData.
	 *
	 * @param referenceDate
	 * @param frequency The default frequency.
	 * @param daycountConvention
	 * @param shortPeriodConvention
	 * @param dateRollConvention
	 * @param businessdayCalendar
	 * @param fixingOffsetDays
	 * @param paymentOffsetDays
	 * @param isUseEndOfMonth
	 */
	public ScheduleMetaData(LocalDate referenceDate, Frequency frequency, DaycountConvention daycountConvention,
			ShortPeriodConvention shortPeriodConvention, DateRollConvention dateRollConvention,
			BusinessdayCalendarInterface businessdayCalendar, int fixingOffsetDays, int paymentOffsetDays,
			boolean isUseEndOfMonth) {
		super();
		this.referenceDate = referenceDate;
		this.frequency = frequency;
		this.daycountConvention = daycountConvention;
		this.shortPeriodConvention = shortPeriodConvention;
		this.dateRollConvention = dateRollConvention;
		this.businessdayCalendar = businessdayCalendar;
		this.fixingOffsetDays = fixingOffsetDays;
		this.paymentOffsetDays = paymentOffsetDays;
		this.isUseEndOfMonth = isUseEndOfMonth;
	}

	/**
	 * Generate a schedule for the given start and end date.
	 *
	 * @param startDate
	 * @param endDate
	 * @return The schedule
	 */
	public ScheduleInterface generateSchedule(LocalDate startDate, LocalDate endDate) {
		return ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, endDate, frequency, daycountConvention,
				shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
	}

	/**
	 * Generate a schedule for the given start and end date. The schedule will consist only of a single period.
	 *
	 * @param startDate
	 * @param endDate
	 * @return The schedule
	 */
	public ScheduleInterface generateSinglePeriod(LocalDate startDate, LocalDate endDate) {
		Frequency frequency = Frequency.TENOR;
		ScheduleInterface schedule =  ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, endDate, frequency, daycountConvention,
				shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
		return schedule;
	}

	/**
	 * Generate a schedule for the given start / end date and frequency.
	 *
	 * @param startDate
	 * @param endDate
	 * @param frequency
	 * @return The schedule
	 */
	public ScheduleInterface generateScheduleWithFrequency(LocalDate startDate, LocalDate endDate, Frequency frequency) {
		return ScheduleGenerator.createScheduleFromConventions(referenceDate, startDate, endDate, frequency, daycountConvention,
				shortPeriodConvention, dateRollConvention, businessdayCalendar, fixingOffsetDays, paymentOffsetDays, isUseEndOfMonth);
	}

	/**
	 * Generate a schedule with start / end date determined by an offset in months from the reference date.
	 *
	 * @param maturity Offset of the start date to the reference date in months
	 * @param termination Offset of the end date to the reference date in months
	 * @return The schedule
	 */
	public ScheduleInterface generateSchedule(int maturity, int termination) {
		return generateSchedule(maturity, termination, OffsetUnit.inMONTHS);
	}

	/**
	 * Generate a schedule with start / end date determined by an offset from the reference date.
	 *
	 * @param maturity Offset of the start date to the reference date
	 * @param termination Offset of the end date to the reference date
	 * @param unit The convention to use for the offset
	 * @return The schedule
	 */
	public ScheduleInterface generateSchedule(int maturity, int termination, OffsetUnit unit) {
		return generateScheduleWithFrequency(maturity, termination, unit, frequency);
	}

	/**
	 * Generate a schedule with a given frequency and start / end date determined by an offset in months from the reference date.
	 *
	 * @param maturity Offset of the start date to the reference date in months
	 * @param termination Offset of the end date to the reference date in months
	 * @param frequency The frequency of the schedule
	 * @return The schedule
	 */
	public ScheduleInterface generateScheduleWithFrequency(int maturity, int termination, Frequency frequency) {
		return generateScheduleWithFrequency(maturity, termination, OffsetUnit.inMONTHS, frequency);
	}

	/**
	 * Generate a schedule with a given frequency and start / end date determined by an offset from the reference date.
	 *
	 * @param maturity Offset of the start date to the reference date in months
	 * @param termination Offset of the end date to the reference date in months
	 * @param unit The convention to use for the offset
	 * @param frequency The frequency of the schedule
	 * @return The schedule
	 */
	public ScheduleInterface generateScheduleWithFrequency(int maturity, int termination, OffsetUnit unit,	Frequency frequency) {

		LocalDate startDate;
		LocalDate endDate;

		switch(unit) {
		case inYEARS :	startDate = referenceDate.plusYears(maturity);		endDate = startDate.plusYears(termination); break;
		case inMONTHS :	startDate = referenceDate.plusMonths(maturity);		endDate = startDate.plusMonths(termination); break;
		case inDAYS :	startDate = referenceDate.plusDays(maturity);		endDate = startDate.plusDays(termination); break;
		case inWEEKS :	startDate = referenceDate.plusDays(maturity *7);	endDate = startDate.plusDays(termination *7); break;
		default :		startDate = referenceDate.plusMonths(maturity);		endDate = startDate.plusMonths(termination); break;
		}

		return generateScheduleWithFrequency(startDate, endDate, frequency);
	}
}

