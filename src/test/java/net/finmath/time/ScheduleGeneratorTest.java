/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
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
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.daycount.DayCountConvention_ACT_360;

/**
 * @author Christian Fries
 */
public class ScheduleGeneratorTest {

	@Test
	public void testScheduleGeneratorMetaData() {
		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(
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

		final Schedule schedule2 = ScheduleGenerator.createScheduleFromConventions(
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

		for(final Period period : schedule.getPeriods()) {
			Assert.assertTrue(schedule2.getPeriods().contains(period));
		}

		Assert.assertEquals("Period start.", schedule2.getPeriod(0).getPeriodStart(), LocalDate.of(2012, 1 + 9, 10 + 2));

		/*
		 * 12.01.2013 is a saturday. End date rolls to 14.01.2013
		 */
		Assert.assertEquals("Period end.", schedule2.getPeriod(0).getPeriodEnd(), LocalDate.of(2013, 1, 14));
		System.out.println(schedule2);
	}

	@Test
	public void testPeriodStartPeriodEnd() {
		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2012, 1, 10) /* referenceDate */,
				LocalDate.of(2012, 1, 12) /* startDate */,
				LocalDate.of(2022, 1, 12) /* maturity */,
				Frequency.QUARTERLY,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.FIRST,
				DateRollConvention.FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0,
				0);

		/*
		 * Period start should equal previous period start
		 */
		LocalDate start = null, end = null;
		for(final Period period : schedule) {
			start = period.getPeriodStart();
			if(end != null) {
				Assert.assertTrue("Period start should equal previous period end.", start.isEqual(end));
			}
			end = period.getPeriodEnd();
		}
	}

	@Test
	public void testPeriodLength() {
		final Schedule schedule = ScheduleGenerator.createScheduleFromConventions(
				LocalDate.of(2012, 1, 10) /* referenceDate */,
				LocalDate.of(2012, 1, 12) /* startDate */,
				LocalDate.of(2022, 1, 12) /* maturity */,
				Frequency.QUARTERLY,
				DaycountConvention.ACT_360,
				ShortPeriodConvention.FIRST,
				DateRollConvention.FOLLOWING,
				new BusinessdayCalendarExcludingTARGETHolidays(),
				0,
				0);

		System.out.println(schedule);

		/*
		 * Period start should equal previous period start
		 */
		for(int periodIndex=0; periodIndex < schedule.getNumberOfPeriods(); periodIndex++) {
			final LocalDate periodStart = schedule.getPeriod(periodIndex).getPeriodStart();
			final LocalDate periodEnd = schedule.getPeriod(periodIndex).getPeriodEnd();
			final double periodLengthDCF = schedule.getPeriodLength(periodIndex);
			final double periodLengthDCFExpected = (new DayCountConvention_ACT_360()).getDaycountFraction(periodStart, periodEnd);
			Assert.assertEquals("Period length re-calculated.", periodLengthDCFExpected, periodLengthDCF, 1E-10);
		}
	}
}
