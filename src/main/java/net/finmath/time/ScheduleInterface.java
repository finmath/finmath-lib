/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.util.ArrayList;
import java.util.Calendar;

import net.finmath.time.daycount.DayCountConventionInterface;

/**
 * Interface of a schedule of interest rate periods with
 * a fixing and payment.
 * 
 * The periods have two representations: one a {@link net.finmath.time.Period}
 * which contains {@link java.util.Calendar} dates and
 * an alternative representation using doubles.
 * 
 * Within a schedule, the mapping from doubles to dates is one to one.
 * 
 * @author Christian Fries
 */
public interface ScheduleInterface extends Iterable<Period> {
	
	/**
	 * Returns the reference data of this schedule.
	 * The reference date is only used to convert dates to doubles using
	 * and internal daycount conventions (ACT/365) which does not need to agree
	 * with the daycount convention used to calculate period length.
	 * 
	 * @return The reference data of this schedule corresponding to t=0.
	 */
	Calendar getReferenceDate();
	
	/**
	 * Returns the array of periods.
	 * 
	 * @return The array of periods.
	 */
	ArrayList<Period> getPeriods();

	/**
	 * Returns the daycount convention used to calculate period lengths.
	 * 
	 * @return The daycount convention used to calculate period lengths.
	 */
	DayCountConventionInterface getDaycountconvention();


	int getNumberOfPeriods();
	
	Period getPeriod(int periodIndex);

	double getFixing(int periodIndex);

	double getPayment(int periodIndex);

	double getPeriodLength(int periodIndex);
}