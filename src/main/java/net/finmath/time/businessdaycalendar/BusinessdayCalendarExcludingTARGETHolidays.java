/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.util.Calendar;

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
	 * @see net.finmath.time.BusinessdayCalendarInterface#isBuisinessday(java.util.Calendar)
	 */
	@Override
	public boolean isBusinessday(Calendar date) {
		int day = date.get(Calendar.DAY_OF_MONTH);
		int month = date.get(Calendar.MONTH)+1;

		Calendar datePlus2 = (Calendar)date.clone();
		datePlus2.add(Calendar.DAY_OF_YEAR, 2);
		
		Calendar dateBefore = (Calendar)date.clone();
		dateBefore.add(Calendar.DAY_OF_YEAR, -1);

		return	(baseCalendar == null || baseCalendar.isBusinessday(date))
				&&	!(day ==  1 && month ==  1)		// date is New Year
				&&	!(day == 25 && month == 12)		// date is Christmas
				&&	!(day == 26 && month == 12)		// date is Boxing Day
				&&	!(day == 31 && month == 12)
				&&	!(day ==  1 && month ==  5)		// date is Labour Dya
				&&	!isEasterSunday(datePlus2)		// date is Good Friday
				&&	!isEasterSunday(dateBefore)		// date is Easter Monday
				;
	}

	/**
	 * Test a given date for being easter sunday.
	 * 
	 * The method uses the algorithms sometimes cited as Meeus,Jones, Butcher Gregorian algorithm.
	 * Take from http://en.wikipedia.org/wiki/Computus
	 * 
	 * @param date The date to check.
	 * @return True, if date is easter sunday.
	 */
	public static boolean isEasterSunday(Calendar date) {
	    int Y = date.get(Calendar.YEAR);
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

		int month = date.get(Calendar.MONTH)+1;
		int day = date.get(Calendar.DAY_OF_MONTH);

	    return (easterSundayMonth == month) && (easterSundayDay == day);
	}
}
