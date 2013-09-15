/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

/**
 * @author Christian Fries
 *
 */
public class RegularSchedule implements ScheduleInterface {

	private TimeDiscretizationInterface timeDiscretization;
	
	/**
	 * 
	 */
	public RegularSchedule(TimeDiscretizationInterface timeDiscretization) {
		this.timeDiscretization = timeDiscretization;
	}

	/* (non-Javadoc)
	 * @see net.finmath.time.ScheduleInterface#getNumberOfPeriods()
	 */
	@Override
	public int getNumberOfPeriods() {
		return timeDiscretization.getNumberOfTimeSteps();
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
}
