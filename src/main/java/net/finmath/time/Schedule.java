/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

import java.time.LocalDate;
import java.util.List;

import net.finmath.time.daycount.DayCountConvention;

/**
 * Interface of a schedule of interest rate periods with
 * a fixing and payment.
 *
 * The periods have two representations: one a {@link net.finmath.time.Period}
 * which contains {@link java.time.LocalDate} dates and
 * an alternative representation using doubles.
 *
 * Within a schedule, the mapping from doubles to dates is one to one.
 *
 * @author Christian Fries
 * @version 1.0
 */
public interface Schedule extends Iterable<Period> {

	/**
	 * Returns the reference data of this schedule.
	 * The reference date is only used to convert dates to doubles using
	 * and internal daycount conventions (ACT/365) which does not need to agree
	 * with the daycount convention used to calculate period length.
	 *
	 * @return The reference data of this schedule corresponding to t=0.
	 */
	LocalDate getReferenceDate();

	/**
	 * Returns the array of periods.
	 *
	 * @return The array of periods.
	 */
	List<Period> getPeriods();

	/**
	 * Returns the daycount convention used to calculate period lengths.
	 *
	 * @return The daycount convention used to calculate period lengths.
	 */
	DayCountConvention getDaycountconvention();

	/**
	 * Returns the number of periods.
	 *
	 * @return The number of periods.
	 */
	int getNumberOfPeriods();

	/**
	 * Return the period for a given period index.
	 *
	 * @param periodIndex A given period index.
	 * @return The period for the given period index.
	 */
	Period getPeriod(int periodIndex);

	/**
	 * Return the fixing converted to the internal daycounting relative
	 * to the schedules reference date.
	 *
	 * @param periodIndex A given period index.
	 * @return The fixing converted to the internal daycounting relative to the schedules reference date.
	 */
	double getFixing(int periodIndex);

	/**
	 * Return the payment date converted to the internal daycounting relative
	 * to the schedules reference date.
	 *
	 * @param periodIndex A given period index.
	 * @return The payment date converted to the internal daycounting relative to the schedules reference date.
	 */
	double getPayment(int periodIndex);

	/**
	 * Return the period start date converted to the internal daycounting relative
	 * to the schedules reference date.
	 *
	 * @param periodIndex A given period index.
	 * @return The period start date converted to the internal daycounting relative to the schedules reference date.
	 */
	double getPeriodStart(int periodIndex);

	/**
	 * Return the period end date converted to the internal daycounting relative
	 * to the schedules reference date.
	 *
	 * @param periodIndex A given period index.
	 * @return The period end date converted to the internal daycounting relative to the schedules reference date.
	 */
	double getPeriodEnd(int periodIndex);

	/**
	 * Return the period length for a given period index.
	 *
	 * @param periodIndex A given period index.
	 * @return The period length for a given period index.
	 */
	double getPeriodLength(int periodIndex);

	/**
	 * Return the index of the period which contains the given time point.
	 *
	 * @param time A given time.
	 * @return The period index of the .
	 */
	int getPeriodIndex(double time);

	/**
	 * Return the index of the period which contains the given date.
	 *
	 * @param date A given date.
	 * @return The period index of the .
	 */
	int getPeriodIndex(LocalDate date);
}
