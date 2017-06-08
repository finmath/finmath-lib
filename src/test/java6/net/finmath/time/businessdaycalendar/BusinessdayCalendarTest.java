package net.finmath.time.businessdaycalendar;

import org.threeten.bp.LocalDate;

import org.junit.Assert;
import org.junit.Test;

/**
 * Provides a few simple tests for Business Day Calendars.
 * 
 * @author Christian Fries
 */
public class BusinessdayCalendarTest {


	/**
	 * Simple tests for date offset codes (1D, 1W, etc.).
	 */
	@Test
	public void testCreateDateFromDateAndOffsetCode() {
		BusinessdayCalendarInterface bdCalendarAny = new BusinessdayCalendarAny();

		LocalDate baseDate = LocalDate.of(2016, 4, 27);

		// Test ACT/365 offsets
		for(float offset : new float[] { 1/365.0f, 2/365.0f, 0.25f, 0.5f, 1.0f }) {
			String offsetCode = Double.toString(offset);
			LocalDate targetDate = bdCalendarAny.createDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays(Math.round(offset*365)).isEqual(targetDate));
		}

		// Test days
		for(int days : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			String offsetCode = Integer.toString(days) + "D";
			LocalDate targetDate = bdCalendarAny.createDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays(days).isEqual(targetDate));
		}

		// Test weeks
		for(int weeks : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			String offsetCode = Integer.toString(weeks) + "W";
			LocalDate targetDate = bdCalendarAny.createDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusWeeks(weeks).isEqual(targetDate));
		}


		// Test month
		for(int months : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			String offsetCode = Integer.toString(months) + "M";
			LocalDate targetDate = bdCalendarAny.createDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusMonths(months).isEqual(targetDate));
		}

		// Test years
		for(int years : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			String offsetCode = Integer.toString(years) + "Y";
			LocalDate targetDate = bdCalendarAny.createDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusYears(years).isEqual(targetDate));
		}

		// Test business days for BusinessdayCalendarAny
		for(int days : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			String offsetCode = Integer.toString(days) + "BD";
			LocalDate targetDate = bdCalendarAny.createDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays(days).isEqual(targetDate));
		}

		// Test business days for BusinessdayCalendarExcludingWeekends
		BusinessdayCalendarInterface bdCalendarWeekends = new BusinessdayCalendarExcludingWeekends();
		for(int days : new int[] { 1, 5, 10, 20, 100 }) {
			String offsetCode = Integer.toString(days) + "BD";
			LocalDate targetDate = bdCalendarWeekends.createDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays((int) Math.round(days / 5.0 * 7.0)).isEqual(targetDate));
		}
	}
	
	/**
	 * Simple tests for TARGET calendar.
	 */
	@Test
	public void testTargetHolidays() {
		BusinessdayCalendarInterface targetCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		
		LocalDate weekend = LocalDate.of(2017, 6, 10);
		Assert.assertTrue(!targetCalendar.isBusinessday(weekend));
		
		LocalDate weekendAndHoliday = LocalDate.of(2017, 1, 1);
		Assert.assertTrue(!targetCalendar.isBusinessday(weekendAndHoliday));
		
		LocalDate newYear = LocalDate.of(2018, 1, 1);
		Assert.assertTrue(!targetCalendar.isBusinessday(newYear));
		
		LocalDate goodFriday = LocalDate.of(2017, 4, 14);
		Assert.assertTrue(!targetCalendar.isBusinessday(goodFriday));
		
		LocalDate easterMonday = LocalDate.of(2017, 4, 17);
		Assert.assertTrue(!targetCalendar.isBusinessday(easterMonday));
		
		LocalDate labourDay = LocalDate.of(2017, 5, 1);
		Assert.assertTrue(!targetCalendar.isBusinessday(labourDay));
		
		LocalDate christmasDay = LocalDate.of(2017, 12, 25);
		Assert.assertTrue(!targetCalendar.isBusinessday(christmasDay));
		
		LocalDate boxingDay = LocalDate.of(2017, 12, 26);
		Assert.assertTrue(!targetCalendar.isBusinessday(boxingDay));
	}
}