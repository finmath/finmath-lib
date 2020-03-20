/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * A business day calendar, where every day is a business day, expect SATURDAY and SUNDAY.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class BusinessdayCalendarExcludingWeekends extends AbstractBusinessdayCalendar {

	/**
	 *
	 */
	private static final long serialVersionUID = -664535279499214108L;
	private BusinessdayCalendar baseCalendar;

	/**
	 * Create business day calendar.
	 */
	public BusinessdayCalendarExcludingWeekends() {
	}

	/**
	 * Create business day calendar using a given business day calendar as basis.
	 *
	 * @param baseCalendar Calendar of business days.
	 */
	public BusinessdayCalendarExcludingWeekends(final BusinessdayCalendar baseCalendar) {
		this.baseCalendar = baseCalendar;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.BusinessdayCalendarInterface#isBuisinessday(java.util.Calendar)
	 */
	@Override
	public boolean isBusinessday(final LocalDate date) {
		return	(baseCalendar == null || baseCalendar.isBusinessday(date))
				&& date.getDayOfWeek() != DayOfWeek.SATURDAY
				&& date.getDayOfWeek() != DayOfWeek.SUNDAY;
	}

	@Override
	public String toString() {
		return "BusinessdayCalendarExcludingWeekends [baseCalendar=" + baseCalendar + "]";
	}
}
