/*
 * (c) Copyright finmath.net, Germany. Contact: info@finmath.net.
 *
 * Created on 01.06.2018
 */
package net.finmath.marketdata.model.bond;

import java.time.LocalDate;

import org.junit.Assert;

import net.finmath.time.FloatingpointDate;
import net.finmath.time.Period;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarAny;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConventionFactory;

public class AccruedInterestTest {

	LocalDate referenceDate= LocalDate.of(2017, 7, 17);
	LocalDate startDate= LocalDate.of(2007, 8, 31);
	LocalDate maturityDate= LocalDate.of(2017, 8, 31);
	String frequency="annual";
	String daycountConvention= "act/act";
	String shortPeriodConvention="FIRST";
	String dateRollConvention="modfollow";
	BusinessdayCalendar businessdayCalendar= new BusinessdayCalendarAny();
	int	fixingOffsetDays=0;
	int	paymentOffsetDays=0;
	Schedule testSchedule= ScheduleGenerator.createScheduleFromConventions(
			referenceDate,
			startDate,
			maturityDate,
			frequency,
			daycountConvention,
			shortPeriodConvention,
			dateRollConvention,
			businessdayCalendar,
			fixingOffsetDays,
			paymentOffsetDays);
	double coupon=5.125;

	private int getPeriodIndex(final LocalDate date) {
		final double floatingDate=FloatingpointDate.getFloatingPointDateFromDate(referenceDate,date);
		if(floatingDate< testSchedule.getPeriodStart(0)|| floatingDate>= testSchedule.getPeriodEnd(testSchedule.getNumberOfPeriods()-1)) {
			return -1;
		}
		for(int i=0; i<testSchedule.getNumberOfPeriods()-1;i++) {
			if(floatingDate<=testSchedule.getPeriodEnd(i)) {
				return i;
			}
		}
		return testSchedule.getNumberOfPeriods()-1;
	}

	private double getAccruedInterest(final LocalDate date) {
		final int periodIndex=getPeriodIndex(date);
		final Period period=testSchedule.getPeriod(periodIndex);
		final DayCountConvention dcc= testSchedule.getDaycountconvention();
		final double accruedInterest=coupon*dcc.getDaycount(period.getPeriodStart(), date)/dcc.getDaycount(period.getPeriodStart(), period.getPeriodEnd());
		return accruedInterest;
	}

	public static void main(final String[] args) {
		final AccruedInterestTest test=new AccruedInterestTest();
		final LocalDate testDate= LocalDate.of(2017, 7, 17);
		System.out.println(test.getAccruedInterest(testDate));
		System.out.println(test.getAccruedInterest(testDate)-4.48843195598473);

		final LocalDate startDate = test.testSchedule.getPeriod(test.getPeriodIndex(testDate)).getPeriodStart();
		final LocalDate endDate = test.testSchedule.getPeriod(test.getPeriodIndex(testDate)).getPeriodEnd();
		final double daysInPeriod = DayCountConventionFactory.getDaycount(startDate, endDate, "act/act isda");
		final double daysAccrual = DayCountConventionFactory.getDaycount(startDate, testDate, "act/act isda");

		final double accruedInterest = test.coupon * 320.0/365.0;

		Assert.assertEquals("accrued interest", accruedInterest, test.getAccruedInterest(testDate), 1E-12);
	}

}
