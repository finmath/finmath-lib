/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_365;

/**
 * A schedule of interest rate periods with
 * a fixing and payment.
 * 
 * The periods have two representations: one a {@link net.finmath.time.Period}
 * which contains {@link java.util.Calendar} dates and
 * an alternative representation using doubles.
 * 
 * Within a schedule, the mapping from doubles to dates is one to one.
 * 
 * @author Christian Fries
 */
public class Schedule implements ScheduleInterface {

	private static	DayCountConventionInterface	internalDayCounting = new DayCountConvention_ACT_365();//ACT_ISDA();
	private			Calendar					referenceDate;
	
	private ArrayList<Period>			periods;
	private DayCountConventionInterface	daycountconvention;
	
	private double[] fixingTimes;
	private double[] paymentTimes;
	private double[] periodLength;
	
	public Schedule(Calendar referenceDate, ArrayList<Period> periods, DayCountConventionInterface daycountconvention) {
		super();
		this.referenceDate = referenceDate;
		this.periods = periods;
		this.daycountconvention = daycountconvention;

		// Precalculate dates to yearfrac doubles
		fixingTimes = new double[periods.size()];
		paymentTimes = new double[periods.size()];
		periodLength = new double[periods.size()];
		for(int periodIndex=0; periodIndex < periods.size(); periodIndex++) {
			fixingTimes[periodIndex] = internalDayCounting.getDaycountFraction(referenceDate, periods.get(periodIndex).getFixing());
			paymentTimes[periodIndex] = internalDayCounting.getDaycountFraction(referenceDate, periods.get(periodIndex).getPayment());
			periodLength[periodIndex] = daycountconvention.getDaycountFraction(periods.get(periodIndex).getPeriodStart(), periods.get(periodIndex).getPeriodEnd());
		}
	}


	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getReferenceDate()
	 */
	@Override
	public Calendar getReferenceDate() {
		return referenceDate;
	}


	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getNumberOfPeriods()
	 */
	@Override
	public int getNumberOfPeriods() {
		return periods.size();
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPeriod(int)
	 */
	@Override
	public Period getPeriod(int periodIndex) {
		return periods.get(periodIndex);
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getFixing(int)
	 */
	@Override
	public double getFixing(int periodIndex) {
		return fixingTimes[periodIndex];
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPayment(int)
	 */
	@Override
	public double getPayment(int periodIndex) {
		return paymentTimes[periodIndex];
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPeriodLength(int)
	 */
	@Override
	public double getPeriodLength(int periodIndex) {
		return periodLength[periodIndex];
	}


	@Override
	public String toString() {
		return "Schedule [referenceDate=" + referenceDate.getTime() + ", periods="
				+ periods + ", daycountconvention=" + daycountconvention + "]";
	}


	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Period> iterator() {
		return periods.iterator();
	}



}
