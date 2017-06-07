/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import org.threeten.bp.LocalDate;

/**
 * A business day calendar, where every day is a business day.
 * 
 * @author Christian Fries
 */
public class BusinessdayCalendarAny extends BusinessdayCalendar {

	/**
	 * Create a business day calendar, where every day is a business day.
	 */
	public BusinessdayCalendarAny() {
	}

	@Override
	public boolean isBusinessday(LocalDate date) {
		return true;
	}

	public String toString() {
		return "BusinessdayCalendarAny";
	}
}
