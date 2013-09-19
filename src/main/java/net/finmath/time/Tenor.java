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
public class Tenor extends TimeDiscretization implements TenorInterface {

	private static final DayCountConventionInterface internalDayCounting = new DayCountConvention_30E_360_ISDA();

	private	GregorianCalendar		referenceDate;
	private GregorianCalendar[]		dates;

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

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getReferenceDate()
	 */
	@Override
	public GregorianCalendar getReferenceDate() {
		return referenceDate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getDate(int)
	 */
	@Override
	public GregorianCalendar getDate(int timeIndex) {
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
