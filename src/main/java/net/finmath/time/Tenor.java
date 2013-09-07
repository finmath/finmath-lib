/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 03.09.2013
 */

package net.finmath.time;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_30_360_ISDA;

/**
 * Implements a time discretization bases on dates using a reference
 * date and an daycount convention / year fraction.
 * 
 * The time as a double is represented as the year fraction from the reference date.
 * 
 * @author Christian Fries
 */
public class Tenor extends TimeDiscretization {

	private static final DayCountConventionInterface internalDayCounting = new DayCountConvention_30_360_ISDA();

	private	Date		referenceDate;

	/**
	 * @param dates
	 * @param referenceDate
	 * @param daycountFractions
	 */
	public Tenor(Date[] dates, Date referenceDate, double[] daycountFractions) {
		super(createTimeDiscretizationFromDates(dates, referenceDate));
		this.referenceDate		= referenceDate;
		
	}

	/**
	 * @param dates
	 * @param referenceDate
	 * @return
	 */
	private static double[] createTimeDiscretizationFromDates(Date[] dates, Date referenceDate) {
		GregorianCalendar calendarStart = (GregorianCalendar) GregorianCalendar.getInstance();
		calendarStart.setTime(referenceDate);
		calendarStart.getTimeInMillis();
		
		double[] timeDiscretization = new double[dates.length];

		GregorianCalendar calendarDate = (GregorianCalendar) GregorianCalendar.getInstance();
		for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++) {
			calendarDate.setTime(dates[timeIndex]);

			timeDiscretization[timeIndex] =
					internalDayCounting.getDaycount(calendarStart, calendarDate);
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

	/**
	 * @return The reference date of this tenor, i.e., the date mapped to 0.0
	 */
	public Date getReferenceDate() {
		return referenceDate;
	}
}
