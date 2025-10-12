/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.09.2013
 */

package net.finmath.time;

import java.time.LocalDate;

/**
 * Implements a time discretization based on dates using a reference
 * date and an daycount convention / year fraction.
 *
 * The time as a double is represented as the year fraction from the reference date.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class TenorFromArray extends TimeDiscretizationFromArray implements Tenor {

	private static final long serialVersionUID = 4027884423439197483L;

	private			LocalDate					referenceDate;
	private 		LocalDate[]					dates;

	/**
	 * @param dates A set of dates.
	 * @param referenceDate The reference date, which defines t=0 with respect to the internal double representation.
	 */
	public TenorFromArray(final LocalDate[] dates, final LocalDate referenceDate) {
		super(createTimeDiscretizationFromDates(dates, referenceDate));
		this.dates				= dates;
		this.referenceDate		= referenceDate;
	}

	/**
	 * @param dates
	 * @param referenceDate
	 * @return A time discretization corresponding to the given dates, relative to the reference date, using the internal day count fraction.
	 */
	private static double[] createTimeDiscretizationFromDates(final LocalDate[] dates, final LocalDate referenceDate) {

		final double[] timeDiscretization = new double[dates.length];

		for(int timeIndex=0; timeIndex<timeDiscretization.length; timeIndex++) {
			timeDiscretization[timeIndex] =
					FloatingpointDate.getFloatingPointDateFromDate(referenceDate, dates[timeIndex]);
		}

		return timeDiscretization;
	}

	/**
	 * Construct a tenor from a time discretization.
	 *
	 * @param timeDiscretization A time discretization.
	 */
	public TenorFromArray(final double[] timeDiscretization) {
		super(timeDiscretization);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Construct a tenor from a time discretization.
	 *
	 * @param timeDiscretization A time discretization.
	 */
	public TenorFromArray(final Double[] timeDiscretization) {
		super(timeDiscretization);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Construct a tenor from meta data.
	 *
	 * @param initial First discretization point.
	 * @param numberOfTimeSteps Number of time steps.
	 * @param deltaT Time step size.
	 */
	public TenorFromArray(final double initial, final int numberOfTimeSteps, final double deltaT) {
		super(initial, numberOfTimeSteps, deltaT);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Construct a tenor from meta data.
	 *
	 * @param initial First discretization point.
	 * @param last Last time steps.
	 * @param deltaT Time step size.
	 * @param shortPeriodLocation Placement of the stub period.
	 */
	public TenorFromArray(final double initial, final double last, final double deltaT, final ShortPeriodLocation shortPeriodLocation) {
		super(initial, last, deltaT, shortPeriodLocation);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.Tenor#getReferenceDate()
	 */
	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.Tenor#getDate(int)
	 */
	@Override
	public LocalDate getDate(final int timeIndex) {
		return dates[timeIndex];
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.Tenor#getDaycountFraction(int)
	 */
	@Override
	public double getDaycountFraction(final int timeIndex) {
		return this.getTimeStep(timeIndex);
	}

	@Override
	public String toString() {
		String datesOutputString = "[";
		for(int iDate=0; iDate<dates.length; iDate++) {
			datesOutputString += dates[iDate].toString() + (iDate==dates.length-1 ? "" : ", ");
		}
		datesOutputString += "]";

		return "TenorFromArray [referenceDate=" + referenceDate + ", dates=" + datesOutputString + "]";
	}
}
