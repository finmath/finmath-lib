/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 15.09.2013
 */

package net.finmath.time.businessdaycalendar;

import org.joda.time.LocalDate;

/**
 * A business day calendar, where every day is a business day.
 * 
 * @author Christian Fries
 */
public class BusinessdayCalendarAny extends BusinessdayCalendar {

	/**
	 * 
	 */
	public BusinessdayCalendarAny() {
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.BusinessdayCalendarInterface#isBuisinessday(org.joda.time.LocalDate)
	 */
	@Override
	public boolean isBusinessday(LocalDate date) {
		return true;
	}

}
