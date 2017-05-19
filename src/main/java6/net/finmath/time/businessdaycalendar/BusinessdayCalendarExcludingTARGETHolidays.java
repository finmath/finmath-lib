/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import org.joda.time.LocalDate;

/**
 * A business day calendar, where every day is a business day, expect
 * the TARGET holidays.
 * 
 * @author Christian Fries
 */
public class BusinessdayCalendarExcludingTARGETHolidays extends BusinessdayCalendar {

	private BusinessdayCalendarInterface baseCalendar;
	
	/**
	 * Create business day calendar.
	 */
	public BusinessdayCalendarExcludingTARGETHolidays() {
		this.baseCalendar = new BusinessdayCalendarExcludingWeekends();
	}

	/**
	 * Create business day calendar using a given business day calendar as basis.
	 * 
	 * @param baseCalendar Calendar of business days.
	 */
	public BusinessdayCalendarExcludingTARGETHolidays(BusinessdayCalendarInterface baseCalendar) {
		this.baseCalendar = new BusinessdayCalendarExcludingWeekends(baseCalendar);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.BusinessdayCalendarInterface#isBuisinessday(java.time.LocalDate)
	 */
	@Override
	public boolean isBusinessday(LocalDate date) {
		int day = date.getDayOfMonth();
		int month = date.getMonthOfYear();

		LocalDate datePlus2 = date.plusDays(2);
		LocalDate dateBefore = date.minusDays(1);

		return	(baseCalendar == null || baseCalendar.isBusinessday(date))
				&&	!(day ==  1 && month ==  1)		// date is New Year
				&&	!(day == 25 && month == 12)		// date is Christmas
				&&	!(day == 26 && month == 12)		// date is Boxing Day
				&&	!(day == 31 && month == 12)
				&&	!(day ==  1 && month ==  5)		// date is Labour Day
				&&	!isEasterSunday(datePlus2)		// date is Good Friday
				&&	!isEasterSunday(dateBefore)		// date is Easter Monday
				;
	}

	/**
	 * Test a given date for being easter sunday.
	 * 
	 * The method uses the algorithms sometimes cited as Meeus,Jones, Butcher Gregorian algorithm.
	 * Taken from http://en.wikipedia.org/wiki/Computus
	 * 
	 * @param date The date to check.
	 * @return True, if date is easter sunday.
	 */
	public static boolean isEasterSunday(LocalDate date) {
	    int Y = date.getYear();
	    int a = Y % 19;
	    int b = Y / 100;
	    int c = Y % 100;
	    int d = b / 4;
	    int e = b % 4;
	    int f = (b + 8) / 25;
	    int g = (b - f + 1) / 3;
	    int h = (19 * a + b - d - g + 15) % 30;
	    int i = c / 4;
	    int k = c % 4;
	    int L = (32 + 2 * e + 2 * i - h - k) % 7;
	    int m = (a + 11 * h + 22 * L) / 451;
	    int easterSundayMonth	= (h + L - 7 * m + 114) / 31;
	    int easterSundayDay		= ((h + L - 7 * m + 114) % 31) + 1;

		int month = date.getMonthOfYear();
		int day = date.getDayOfMonth();

	    return (easterSundayMonth == month) && (easterSundayDay == day);
	}
}
