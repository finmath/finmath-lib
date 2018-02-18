/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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

	private static final long serialVersionUID = -2440422998196510638L;

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
