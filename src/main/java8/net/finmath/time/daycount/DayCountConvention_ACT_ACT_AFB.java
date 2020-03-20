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
 * Implementation of ACT/ACT AFB.
 *
 * Calculates the day count by calculating the actual number of days between startDate and endDate.
 *
 * A fractional day is rounded to the approximately nearest day.
 *
 * The day count fraction is calculated using ACT_ACT_AFB convention. The algorithm works as follows:
 * <ul>
 * 	<li>
 * 		If the interval from startDate to endDate spans more than a whole year, then
 * 		the number of whole years is subtracted from the endDate year and added (as an integer) to the day count fraction.
 * 		Here, subtraction of whole year(s) means:
 * 		<ul>
 * 			<li>
 * 				If the end date is on a February 29th and the resulting day is not in a leap year,
 * 				it will be set to February 28th of that year (i.e., we preserve "end of month").
 * 			</li>
 * 			<li>
 * 				If the end date is on a February 28th of a non-leap year and the resulting day is in a leap year,
 * 				it will be on February 29th of that year (i.e., we preserve "end of month").
 * 			</li>
 * 			<li>
 * 				Otherwise the resulting day has the same day of month and the same month as the end year.
 * 			</li>
 * 		</ul>
 * 	</li>
 * 	<li>
 * 		For the remaining interval the actual number of days is divided by a denominator,
 * 		where the denominator is 366.0 if February 29th is in between start and end and 365.0 otherwise.
 * 	</li>
 * </ul>
 *
 * @author Christian Fries
 * @version 1.0
 */
public class DayCountConvention_ACT_ACT_AFB extends DayCountConvention_ACT implements Serializable {

	private static final long serialVersionUID = 3362135261620939045L;

	/**
	 * Create an ACT/ACT FBA daycount convention.
	 */
	public DayCountConvention_ACT_ACT_AFB() {
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
		 * Find the "fractionalPeriodEnd", i.e. subtract whole years from endDate.
		 */
		LocalDate fractionalPeriodEnd = endDate.plusYears(startDate.getYear() - endDate.getYear());

		// preserving 'end-of-month' if endDate is 28/Feb of non-leap-year or 29/Feb of non-leap-year.
		if (endDate.getDayOfMonth() == endDate.lengthOfMonth()) {
			fractionalPeriodEnd = fractionalPeriodEnd.withDayOfMonth(fractionalPeriodEnd.lengthOfMonth());
		}

		if (fractionalPeriodEnd.isBefore(startDate)) {
			fractionalPeriodEnd.plusYears(1);
			// preserving 'end-of-month' if endDate is 28/Feb of non-leap-year or 29/Feb of non-leap-year, again after changing the years.
			if (endDate.getDayOfMonth() == endDate.lengthOfMonth()) {
				fractionalPeriodEnd = fractionalPeriodEnd.withDayOfMonth(fractionalPeriodEnd.lengthOfMonth());
			}
		}

		double daycountFraction = endDate.getYear() - fractionalPeriodEnd.getYear();

		double fractionPeriodDenominator = 365.0;
		if(fractionalPeriodEnd.isLeapYear()) {
			final LocalDate feb29th = LocalDate.of(fractionalPeriodEnd.getYear(), Month.FEBRUARY, 29);
			if(startDate.compareTo(feb29th) <= 0 && fractionalPeriodEnd.compareTo(feb29th) > 0) {
				fractionPeriodDenominator = 366.0;
			}
		}
		else if(startDate.isLeapYear()) {
			final LocalDate feb29th = LocalDate.of(startDate.getYear(), Month.FEBRUARY, 29);
			if(startDate.compareTo(feb29th) <= 0 && fractionalPeriodEnd.compareTo(feb29th) > 0) {
				fractionPeriodDenominator = 366.0;
			}
		}

		daycountFraction += getDaycount(startDate, fractionalPeriodEnd) / fractionPeriodDenominator;

		return daycountFraction;
	}

	@Override
	public String toString() {
		return "DayCountConvention_ACT_ACT_AFB";
	}

}
