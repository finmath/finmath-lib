/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import java.time.LocalDate;

/**
 * A business day calendar, where every day is a business day.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class BusinessdayCalendarAny extends AbstractBusinessdayCalendar {

	private static final long serialVersionUID = -2440422998196510638L;

	/**
	 * Create a business day calendar, where every day is a business day.
	 */
	public BusinessdayCalendarAny() {
	}

	@Override
	public boolean isBusinessday(final LocalDate date) {
		return true;
	}

	@Override
	public String toString() {
		return "BusinessdayCalendarAny";
	}
}
