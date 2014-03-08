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
	public Calendar getReferenceDate() {
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

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getNumberOfPeriods()
	 */
	@Override
	public int getNumberOfPeriods() {
		return timeDiscretization.getNumberOfTimeSteps();
	}
	
	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPeriod(int)
	 */
	@Override
	public Period getPeriod(int periodIndex) {
		return null;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getFixing(int)
	 */
	@Override
	public double getFixing(int periodIndex) {
		return timeDiscretization.getTime(periodIndex);
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPayment(int)
	 */
	@Override
	public double getPayment(int periodIndex) {
		return timeDiscretization.getTime(periodIndex+1);
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getPeriodLength(int)
	 */
	@Override
	public double getPeriodLength(int periodIndex) {
		return timeDiscretization.getTimeStep(periodIndex);
	}

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Period> iterator() {
		// TODO Auto-generated method stub
		return null;
	}
}
