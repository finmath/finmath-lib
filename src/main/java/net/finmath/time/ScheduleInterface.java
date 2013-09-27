/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.09.2013
 */

package net.finmath.time;

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

	int getNumberOfPeriods();
	
	Period getPeriod(int periodIndex);

	double getFixing(int periodIndex);

	double getPayment(int periodIndex);

	double getPeriodLength(int periodIndex);
}