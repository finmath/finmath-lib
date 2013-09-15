/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_30E_360;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;;

/**
 * @author Christian Fries
 */
public class Schedule implements ScheduleInterface {

	private static final DayCountConventionInterface internalDayCounting = new DayCountConvention_ACT_ACT_ISDA();

	private	Calendar					referenceDate;

	private ArrayList<Period>			periods;
	private DayCountConventionInterface	daycountconvention;
	

	public Schedule(Calendar referenceDate, ArrayList<Period> periods, DayCountConventionInterface daycountconvention) {
		super();
		this.referenceDate = referenceDate;
		this.periods = periods;
		this.daycountconvention = daycountconvention;
	}


	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getNumberOfPeriods()
	 */
	@Override
	public int getNumberOfPeriods() {
		return periods.size();
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getFixing(int)
	 */
	@Override
	public double getFixing(int periodIndex) {
		return internalDayCounting.getDaycountFraction(referenceDate, periods.get(periodIndex).getFixing());
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPayment(int)
	 */
	@Override
	public double getPayment(int periodIndex) {
		return internalDayCounting.getDaycountFraction(referenceDate, periods.get(periodIndex).getPayment());
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPeriodLength(int)
	 */
	@Override
	public double getPeriodLength(int periodIndex) {
		return daycountconvention.getDaycountFraction(periods.get(periodIndex).getPeriodStart(), periods.get(periodIndex).getPeriodEnd());
	}


	@Override
	public String toString() {
		return "Schedule [referenceDate=" + referenceDate.getTime() + ", periods="
				+ periods + ", daycountconvention=" + daycountconvention + "]";
	}
	
}
