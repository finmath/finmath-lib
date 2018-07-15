/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 03.09.2013
 */

package net.finmath.time;

import org.threeten.bp.LocalDate;

/**
 * Implements a time discretization based on dates using a reference
 * date and an daycount convention / year fraction.
 *
 * The time as a double is represented as the year fraction from the reference date.
 *
 * @author Christian Fries
 */
public class Tenor extends TimeDiscretization implements TenorInterface {

	private static final long serialVersionUID = 4027884423439197483L;

	private			LocalDate					referenceDate;
	private 		LocalDate[]					dates;

	/**
	 * @param dates A set of dates.
	 * @param referenceDate The reference date, which defines t=0 with respect to the internal double representation.
	 */
	public Tenor(LocalDate[] dates, LocalDate referenceDate) {
		super(createTimeDiscretizationFromDates(dates, referenceDate));
		this.dates				= dates;
		this.referenceDate		= referenceDate;
	}

	/**
	 * @param dates
	 * @param referenceDate
	 * @return A time discretization corresponding to the given dates, relative to the reference date, using the internal day count fraction.
	 */
	private static double[] createTimeDiscretizationFromDates(LocalDate[] dates, LocalDate referenceDate) {

		double[] timeDiscretization = new double[dates.length];

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
	public Tenor(double[] timeDiscretization) {
		super(timeDiscretization);
		// TODO Auto-generated constructor stub
	}

	/**
	 * Construct a tenor from a time discretization.
	 *
	 * @param timeDiscretization A time discretization.
	 */
	public Tenor(Double[] timeDiscretization) {
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
	public Tenor(double initial, int numberOfTimeSteps, double deltaT) {
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
	public Tenor(double initial, double last, double deltaT, ShortPeriodLocation shortPeriodLocation) {
		super(initial, last, deltaT, shortPeriodLocation);
		// TODO Auto-generated constructor stub
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getReferenceDate()
	 */
	@Override
	public LocalDate getReferenceDate() {
		return referenceDate;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getDate(int)
	 */
	@Override
	public LocalDate getDate(int timeIndex) {
		return dates[timeIndex];
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.TenorInterface#getDaycountFraction(int)
	 */
	@Override
	public double getDaycountFraction(int timeIndex) {
		return this.getTimeStep(timeIndex);
	}

	@Override
	public String toString() {
		String datesOutputString = "[";
		for(int iDate=0; iDate<dates.length; iDate++) {
			datesOutputString += dates[iDate].toString() + (iDate==dates.length-1 ? "" : ", ");
		}
		datesOutputString += "]";

		return "Tenor [referenceDate=" + referenceDate + ", dates=" + datesOutputString + "]";
	}
}
