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
public interface ScheduleInterface {

	public abstract int getNumberOfPeriods();

	public abstract double getFixing(int periodIndex);

	public abstract double getPayment(int periodIndex);

	public abstract double getPeriodLength(int periodIndex);

}