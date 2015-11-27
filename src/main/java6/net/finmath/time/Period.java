/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import org.joda.time.LocalDate;

/**
 * A period, i.e. a time interval suitable for securities with regular payment schedules.
 * 
 * For example, the accrual period is usually given by the interval periodStart, periodEnd, where as fixing date and payment date
 * can be adjusted versions of periodStart and periodsEnd, e.g. via fixing offsets and payment offsets.
 * 
 * Period implement the <code>Comparable</code> interface by simply using getPeriodEnd().compareTo(), i.e., the ordering is
 * determined by periodEnd only.
 * 
 * For a list of subsequent (sorted) periods it is often assumed that periodStart agrees with the periodEnd of the preceeding period,
 * resulting in a time-discretization.
 * 
 * @author Christian Fries
 *
 */
public class Period implements Comparable<Period> {

	private LocalDate fixing;
	private LocalDate payment;
	private LocalDate periodStart;
	private LocalDate periodEnd;

	public Period(LocalDate fixing,  LocalDate payment,
			LocalDate periodStart, LocalDate periodEnd) {
		super();
		this.fixing = fixing;
		this.payment = payment;
		this.periodStart = periodStart;
		this.periodEnd = periodEnd;
	}

	public LocalDate getFixing() {
		return fixing;
	}

	public LocalDate getPayment() {
		return payment;
	}

	public LocalDate getPeriodStart() {
		return periodStart;
	}

	public LocalDate getPeriodEnd() {
		return periodEnd;
	}

	@Override
	public int compareTo(Period o) {
		return getPeriodEnd().compareTo(o.getPeriodEnd());
	}

	@Override
	public String toString() {
		return "Period [fixing=" + fixing + ", payment=" + payment
				+ ", periodStart=" + periodStart + ", periodEnd=" + periodEnd
				+ "]";
	}
}
