/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;

import net.finmath.time.daycount.DayCountConvention;

/**
 * Simple schedule generated from {@link net.finmath.time.TimeDiscretization}
 *
 * @author Christian Fries
 * @version 1.0
 */
public class RegularSchedule implements Schedule {

	private final TimeDiscretization timeDiscretization;

	/**
	 * Create a schedule from a time discretization.
	 *
	 * @param timeDiscretization The time discretization.
	 */
	public RegularSchedule(final TimeDiscretization timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	@Override
	public LocalDate getReferenceDate() {
		return null;
	}

	@Override
	public ArrayList<Period> getPeriods() {
		return null;
	}

	@Override
	public DayCountConvention getDaycountconvention() {
		return null;
	}

	@Override
	public int getNumberOfPeriods() {
		return timeDiscretization.getNumberOfTimeSteps();
	}

	@Override
	public Period getPeriod(final int periodIndex) {
		return null;
	}

	@Override
	public double getFixing(final int periodIndex) {
		return timeDiscretization.getTime(periodIndex);
	}

	@Override
	public double getPayment(final int periodIndex) {
		return timeDiscretization.getTime(periodIndex+1);
	}

	@Override
	public double getPeriodStart(final int periodIndex) {
		return timeDiscretization.getTime(periodIndex);
	}

	@Override
	public double getPeriodEnd(final int periodIndex) {
		return timeDiscretization.getTime(periodIndex+1);
	}

	@Override
	public double getPeriodLength(final int periodIndex) {
		return timeDiscretization.getTimeStep(periodIndex);
	}

	@Override
	public Iterator<Period> iterator() {
		return null;
	}

	@Override
	public int getPeriodIndex(final double time) {
		return timeDiscretization.getTimeIndex(time);
	}

	@Override
	public int getPeriodIndex(final LocalDate date) {
		final double time= FloatingpointDate.getFloatingPointDateFromDate(getReferenceDate(), date);
		return getPeriodIndex(time);
	}
}
