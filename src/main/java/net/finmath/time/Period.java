/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.finmath.time.daycount.DayCountConventionInterface;

/**
 * @author Christian Fries
 *
 */
public class Period {

	private Calendar fixing;
	private Calendar payment;
	private Calendar periodStart;
	private Calendar periodEnd;

	public Period(Calendar fixing, Calendar payment,
			Calendar periodStart, Calendar periodEnd) {
		super();
		this.fixing = fixing;
		this.payment = payment;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
	}

	public Calendar getFixing() {
		return fixing;
	}

	public Calendar getPayment() {
		return payment;
	}

	public Calendar getPeriodStart() {
		return periodStart;
	}

	public Calendar getPeriodEnd() {
		return periodEnd;
	}

	@Override
	public String toString() {
		return "Period [fixing=" + fixing.getTime() + ", payment=" + payment.getTime()
				+ ", periodStart=" + periodStart.getTime() + ", periodEnd=" + periodEnd.getTime()
				+ "]";
	}
}
