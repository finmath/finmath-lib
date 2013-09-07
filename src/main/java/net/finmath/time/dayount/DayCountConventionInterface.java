/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time.dayount;

import java.util.GregorianCalendar;

/**
 * @author Christian Fries
 *
 */
public interface DayCountConventionInterface {

	public abstract double getDaycount(GregorianCalendar startDate, GregorianCalendar endDate);
	public abstract double getDaycountFraction(GregorianCalendar startDate, GregorianCalendar endDate);

}