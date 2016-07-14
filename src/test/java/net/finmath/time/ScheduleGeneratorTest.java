/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 01.12.2015
 */

package net.finmath.time;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.time.ScheduleGenerator.DaycountConvention;
import net.finmath.time.ScheduleGenerator.Frequency;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;

/**
 * @author Christian Fries
 */
public class ScheduleGeneratorTest {

	@Test
	public void test() {
		ScheduleInterface schedule = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2012, 1, 10) /* referenceDate */,
				LocalDate.of(2012, 10, 12) /* startDate */,
				LocalDate.of(2013, 1, 12) /* maturity */,
				Frequency.QUARTERLY,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.FIRST,
				DateRollConvention.FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0,
				0);

		System.out.println(schedule);

		ScheduleInterface schedule2 = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2012, 1, 10) /* referenceDate */,
				"9M 2D" /* startOffset */,
				"3M" /* maturity */,
				"quarterly" /* frequency */,
				"act/360" /* daycountConvention */,
				"first" /* shortPeriodConvention */,
				"following",
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0,
				0);

		for(Period period : schedule.getPeriods()) {
			Assert.assertTrue(schedule2.getPeriods().contains(period));
		}

		Assert.assertTrue("Period start.", schedule2.getPeriod(0).getPeriodStart().equals(LocalDate.of(2012, 1+9, 10+2)));
		
		/*
		 * 12.01.2013 is a saturday. End date rolls to 14.01.2013
		 */
		Assert.assertTrue("Period end.", schedule2.getPeriod(0).getPeriodEnd().equals(LocalDate.of(2013, 01, 14)));
		System.out.println(schedule2);
	}

}
