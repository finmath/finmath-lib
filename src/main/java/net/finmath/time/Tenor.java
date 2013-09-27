/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 03.09.2013
 */

package net.finmath.time;

import java.util.Calendar;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;

/**
 * Implements a time discretization bases on dates using a reference
 * date and an daycount convention / year fraction.
 * 
 * The time as a double is represented as the year fraction from the reference date.
 * 
 * @author Christian Fries
 */
public class Tenor extends TimeDiscretization implements TenorInterface {

	private static final long serialVersionUID = 4027884423439197483L;

	private static	DayCountConventionInterface	internalDayCounting = new DayCountConvention_ACT_ACT_ISDA();
	private			Calendar					referenceDate;

	private Calendar[]		dates;

	/**
	 * @param dates A set of dates.
	 * @param referenceDate The reference date, which defines t=0 with respect to the internal double representation.
	 */
	public Tenor(Calendar[] dates, Calendar referenceDate) {
		super(createTimeDiscretizationFromDates(dates, referenceDate));
		this.dates				= dates;
		this.referenceDate		= referenceDate;
	}

	/**
	 * @param dates
	 * @param referenceDate
	 * @return
	 */
	private static double[] createTimeDiscretizationFromDates(Calendar[] dates, Calendar referenceDate) {

		double[] timeDiscretization = new double[dates.length];

		for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++) {
			timeDiscretization[timeIndex] =
					internalDayCounting.getDaycount(referenceDate, dates[timeIndex]);
		}

		return timeDiscretization;
	}

	/**
	 * @param timeDiscretization
	 */
	public Tenor(double[] timeDiscretization) {
		super(timeDiscretization);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param timeDiscretization
	 */
	public Tenor(Double[] timeDiscretization) {
		super(timeDiscretization);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param initial
	 * @param numberOfTimeSteps
	 * @param deltaT
	 */
	public Tenor(double initial, int numberOfTimeSteps, double deltaT) {
		super(initial, numberOfTimeSteps, deltaT);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param initial
	 * @param last
	 * @param deltaT
	 * @param shortPeriodLocation
	 */
	public Tenor(double initial, double last, double deltaT,
			ShortPeriodLocation shortPeriodLocation) {
		super(initial, last, deltaT, shortPeriodLocation);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getReferenceDate()
	 */
	@Override
	public Calendar getReferenceDate() {
		return referenceDate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getDate(int)
	 */
	@Override
	public Calendar getDate(int timeIndex) {
		return dates[timeIndex];
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getDaycountFraction(int)
	 */
	@Override
	public double getDaycountFraction(int timeIndex) {
		return this.getTimeStep(timeIndex);
	}
}
