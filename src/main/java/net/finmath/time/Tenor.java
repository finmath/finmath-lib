/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 03.09.2013
 */

package net.finmath.time;

import java.util.GregorianCalendar;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_30E_360_ISDA;

/**
 * Implements a time discretization bases on dates using a reference
 * date and an daycount convention / year fraction.
 * 
 * The time as a double is represented as the year fraction from the reference date.
 * 
 * @author Christian Fries
 */
public class Tenor extends TimeDiscretization {

	private static final DayCountConventionInterface internalDayCounting = new DayCountConvention_30E_360_ISDA();

	private GregorianCalendar[]		dates;
	private	GregorianCalendar		referenceDate;

	/**
	 * @param dates
	 * @param referenceDate
	 * @param daycountFractions
	 */
	public Tenor(GregorianCalendar[] dates, GregorianCalendar referenceDate, DayCountConventionInterface daycountConvention) {
		super(createTimeDiscretizationFromDates(dates, referenceDate, daycountConvention));
		this.dates				= dates;
		this.referenceDate		= referenceDate;
		
	}

	/**
	 * @param dates
	 * @param referenceDate
	 * @return
	 */
	private static double[] createTimeDiscretizationFromDates(GregorianCalendar[] dates, GregorianCalendar referenceDate, DayCountConventionInterface daycountConvention) {

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

	/**
	 * @return The reference date of this tenor, i.e., the date mapped to 0.0
	 */
	public GregorianCalendar getReferenceDate() {
		return referenceDate;
	}

	/**
	 * Returns the date for the given time index.
	 * 
	 * @param timeIndex Time index
	 * @return Returns the date for a given time index.
	 */
	public GregorianCalendar getDate(int timeIndex) {
		return timeIndex == 0 ? referenceDate : dates[timeIndex-1];
	}
}
