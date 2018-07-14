/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.util.ArrayList;
import java.util.Iterator;

import org.threeten.bp.LocalDate;

import net.finmath.time.daycount.DayCountConventionInterface;

/**
 * Simple schedule generated from {@link net.finmath.time.TimeDiscretizationInterface}
 *
 * @author Christian Fries
 */
public class RegularSchedule implements ScheduleInterface {

	private TimeDiscretizationInterface timeDiscretization;

	/**
	 * Create a schedule from a time discretization.
	 *
	 * @param timeDiscretization The time discretization.
	 */
	public RegularSchedule(TimeDiscretizationInterface timeDiscretization) {
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
	public DayCountConventionInterface getDaycountconvention() {
		return null;
	}

	@Override
	public int getNumberOfPeriods() {
		return timeDiscretization.getNumberOfTimeSteps();
	}

	@Override
	public Period getPeriod(int periodIndex) {
		return null;
	}

	@Override
	public double getFixing(int periodIndex) {
		return timeDiscretization.getTime(periodIndex);
	}

	@Override
	public double getPayment(int periodIndex) {
		return timeDiscretization.getTime(periodIndex+1);
	}

	@Override
	public double getPeriodStart(int periodIndex) {
		return timeDiscretization.getTime(periodIndex);
	}

	@Override
	public double getPeriodEnd(int periodIndex) {
		return timeDiscretization.getTime(periodIndex+1);
	}

	@Override
	public double getPeriodLength(int periodIndex) {
		return timeDiscretization.getTimeStep(periodIndex);
	}

	@Override
	public Iterator<Period> iterator() {
		return null;
	}
}
