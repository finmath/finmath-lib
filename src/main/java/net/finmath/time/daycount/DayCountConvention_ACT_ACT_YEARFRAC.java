/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.daycount;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.Month;

/**
 * Implementation of ACT/ACT as in Excel (2013).
 *
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * The day count fraction is calculated using ACT_ACT_YEARFRAC convention, that is, it is calculates the daycount fraction
 * corresponding to the Excel (2013) function YEARFRAC(startDate, endDate, 1). The day count fraction is calculated by dividing
 * the actual number of days between start and end date by a denominator, where the denominator is determines as follows:
 * <ul>
 * 		<li>If the interval from start to end spans more than one year fraction, denominator is the average number of days per year</li>
 * 		<li>If the interval from start to end spans less or equal one year and if start and end fall in a leap year, denominator is 366.0</li>
 * 		<li>If the interval from start to end spans less or equal one year and if either falls in a leap year and if February 29th of that leap year lies in between start and end (or is equal to start or end), denominator is 366.0</li>
 * 		<li>If the interval from start to end spans less or equal one year and neither of the above applied denominator is 365.0</li>
 * </ul>
 *
 * <p>
 * 	<strong>
 * 		This method is not identical to ACT/ACT ISDA. For ACT/ACT ISDA use {@link net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA}.
 * 	</strong>
 * </p>
 *
 * <p>
 * 		In addition the method has the defect, that it is not additive (if you consider small intervals). For example
 *		YEARFRAC(30.12.2011, 04.01.2012, 1) is not equal to YEARFRAC(30.12.2011, 01.01.2012, 1) + YEARFRAC(01.01.2012, 04.01.2012, 1).
 * </p>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DayCountConvention_ACT_ACT_YEARFRAC extends DayCountConvention_ACT implements Serializable {

	private static final long serialVersionUID = -970929056011501752L;

	/**
	 * Create an ACT/ACT YEARFRAC daycount convention.
	 */
	public DayCountConvention_ACT_ACT_YEARFRAC() {
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.daycount.DayCountConvention#getDaycountFraction(java.time.LocalDate, java.time.LocalDate)
	 */
	@Override
	public double getDaycountFraction(final LocalDate startDate, final LocalDate endDate) {
		if(startDate.isAfter(endDate)) {
			return -getDaycountFraction(endDate,startDate);
		}


		/*
		 * Determine the denominator of act/act.
		 */
		double denominator;

		final LocalDate startDatePlusOneYear = startDate.plusYears(1);
		if(endDate.isAfter(startDatePlusOneYear)) {
			/*
			 * The following method applies, if the interval spans more than one year:
			 * fraction = actual number of days / average number of days per year.
			 */

			final LocalDate startDateYearStart = startDate.withDayOfYear(1);
			final LocalDate endDateYearEnd = endDate.withDayOfYear(endDate.lengthOfYear()).plusDays(1);

			final double spannedYears = endDate.getYear() - startDate.getYear() + 1;
			denominator = getDaycount(startDateYearStart, endDateYearEnd) / spannedYears;
		}
		else {
			final boolean isStartLeapYear	= startDate.isLeapYear();
			final boolean isEndLeapYear	= endDate.isLeapYear();
			/*
			 * If the start and end span less or equal one year:
			 * If start and end fall in a leap year, use ACT/366.
			 */
			if(isStartLeapYear && isEndLeapYear)
			{
				denominator = 366.0;
			}
			else
				/*
				 * If the start and end span less or equal one year:
				 * If either falls in a leap year and Feb 29th of that leap year lies in between start and end (or is equal to start or end), use ACT/366.
				 */
				if(isStartLeapYear || isEndLeapYear)
				{
					// Get February 29th of the respective leap year
					final LocalDate leapYearsFeb29th = isStartLeapYear ?
							LocalDate.of(startDate.getYear(), Month.FEBRUARY, 29) :
								LocalDate.of(endDate.getYear(), Month.FEBRUARY, 29);


							// Check position of February 29th
							if(startDate.compareTo(leapYearsFeb29th) <= 0 && endDate.compareTo(leapYearsFeb29th) >= 0) {
								denominator = 366.0;
							}
							else {
								denominator = 365.0;
							}
				}
				else {
					/*
					 * If the start and end span less or equal one year:
					 * If none of the above applies, use denominator = ACT/365.
					 */
					denominator = 365.0;
				}
		}

		final double daycountFraction = getDaycount(startDate, endDate) / denominator;

		return daycountFraction;
	}

	@Override
	public String toString() {
		return "DayCountConvention_ACT_ACT_YEARFRAC";
	}

}
