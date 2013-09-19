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

	int getNumberOfPeriods();
	
	Period getPeriod(int periodIndex);

	double getFixing(int periodIndex);

	double getPayment(int periodIndex);

	double getPeriodLength(int periodIndex);

}