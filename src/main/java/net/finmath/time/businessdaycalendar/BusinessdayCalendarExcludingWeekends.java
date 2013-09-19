/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.util.Calendar;

/**
 * A business day calendar, where every day is a business day, expect SATURDAY and SUNDAY.
 * 
 * @author Christian Fries
 */
public class BusinessdayCalendarExcludingWeekends extends BusinessdayCalendar {

	private BusinessdayCalendarInterface baseCalendar;
	
	/**
	 * 
	 */
	public BusinessdayCalendarExcludingWeekends() {
	}

	public BusinessdayCalendarExcludingWeekends(BusinessdayCalendarInterface baseCalendar) {
		this.baseCalendar = baseCalendar;
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.BusinessdayCalendarInterface#isBuisinessday(java.util.Calendar)
	 */
	@Override
	public boolean isBusinessday(Calendar date) {
		return	(baseCalendar == null || baseCalendar.isBusinessday(date))
			&&	date.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY
			&&	date.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY;
	}

}
