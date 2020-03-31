package net.finmath.time.businessdaycalendar;

import java.time.LocalDate;

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
		final BusinessdayCalendar bdCalendarAny = new BusinessdayCalendarAny();

		final LocalDate baseDate = LocalDate.of(2016, 4, 27);

		// Test ACT/365 offsets
		for(final float offset : new float[] { 1/365.0f, 2/365.0f, 0.25f, 0.5f, 1.0f }) {
			final String offsetCode = Double.toString(offset);
			final LocalDate targetDate = bdCalendarAny.getDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays(Math.round(offset*365)).isEqual(targetDate));
		}

		// Test days
		for(final int days : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			final String offsetCode = Integer.toString(days) + "D";
			final LocalDate targetDate = bdCalendarAny.getDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays(days).isEqual(targetDate));
		}

		// Test weeks
		for(final int weeks : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			final String offsetCode = Integer.toString(weeks) + "W";
			final LocalDate targetDate = bdCalendarAny.getDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusWeeks(weeks).isEqual(targetDate));
		}


		// Test month
		for(final int months : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			final String offsetCode = Integer.toString(months) + "M";
			final LocalDate targetDate = bdCalendarAny.getDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusMonths(months).isEqual(targetDate));
		}

		// Test years
		for(final int years : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			final String offsetCode = Integer.toString(years) + "Y";
			final LocalDate targetDate = bdCalendarAny.getDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusYears(years).isEqual(targetDate));
		}

		// Test business days for BusinessdayCalendarAny
		for(final int days : new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 20, 100 }) {
			final String offsetCode = Integer.toString(days) + "BD";
			final LocalDate targetDate = bdCalendarAny.getDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays(days).isEqual(targetDate));
		}

		// Test business days for BusinessdayCalendarExcludingWeekends
		final BusinessdayCalendar bdCalendarWeekends = new BusinessdayCalendarExcludingWeekends();
		for(final int days : new int[] { 1, 5, 10, 20, 100 }) {
			final String offsetCode = Integer.toString(days) + "BD";
			final LocalDate targetDate = bdCalendarWeekends.getDateFromDateAndOffsetCode(baseDate, offsetCode);
			Assert.assertTrue(baseDate.plusDays((int) Math.round(days / 5.0 * 7.0)).isEqual(targetDate));
		}
	}

	/**
	 * Simple tests for combined calendar.
	 */
	@Test
	public void testCombinedCalendar() {
		final BusinessdayCalendar targetCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		final BusinessdayCalendar targetNewYorkCalendar = new BusinessdayCalendarExcludingNYCHolidays(targetCalendar);
		final BusinessdayCalendar combinedCalendar = new BusinessdayCalendarExcludingLONHolidays(targetNewYorkCalendar);

		// busDay
		LocalDate dateToTest = LocalDate.of(2017, 6, 12);
		Assert.assertTrue(combinedCalendar.isBusinessday(dateToTest));
		// weekend
		dateToTest = LocalDate.of(2017, 6, 10);
		Assert.assertTrue(!combinedCalendar.isBusinessday(dateToTest));
		// weekendAndHoliday
		dateToTest = LocalDate.of(2017, 1, 1);
		Assert.assertTrue(!combinedCalendar.isBusinessday(dateToTest));
		// labourDay
		dateToTest = LocalDate.of(2017, 5, 1);
		Assert.assertTrue(!combinedCalendar.isBusinessday(dateToTest));
		// independence day
		dateToTest = LocalDate.of(2017, 7, 4);
		Assert.assertTrue(!combinedCalendar.isBusinessday(dateToTest));
		// summer bank holiday
		dateToTest = LocalDate.of(2017, 8, 28);
		Assert.assertTrue(!combinedCalendar.isBusinessday(dateToTest));
	}

	/**
	 * Simple tests for TARGET calendar.
	 */
	@Test
	public void testTargetCalendar() {
		final BusinessdayCalendar targetCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		// busDay
		LocalDate dateToTest = LocalDate.of(2017, 6, 12);
		Assert.assertTrue(targetCalendar.isBusinessday(dateToTest));
		// weekend
		dateToTest = LocalDate.of(2017, 6, 10);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
		// weekendAndHoliday
		dateToTest = LocalDate.of(2017, 1, 1);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
		// newYear
		dateToTest = LocalDate.of(2018, 1, 1);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
		// goodFriday
		dateToTest = LocalDate.of(2017, 4, 14);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
		// easterMonday
		dateToTest = LocalDate.of(2017, 4, 17);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
		// labourDay
		dateToTest = LocalDate.of(2017, 5, 1);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
		// christmasDay
		dateToTest = LocalDate.of(2017, 12, 25);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
		// boxingDay
		dateToTest = LocalDate.of(2017, 12, 26);
		Assert.assertTrue(!targetCalendar.isBusinessday(dateToTest));
	}

	/**
	 * Simple tests for NYC calendar.
	 */
	@Test
	public void testNycCalendar() {
		final BusinessdayCalendar newYorkCalendar = new BusinessdayCalendarExcludingNYCHolidays();
		// busDay
		LocalDate dateToTest = LocalDate.of(2017, 6, 12);
		Assert.assertTrue(newYorkCalendar.isBusinessday(dateToTest));
		// weekend
		dateToTest = LocalDate.of(2017, 6, 10);
		Assert.assertTrue(!newYorkCalendar.isBusinessday(dateToTest));
		// independence day
		dateToTest = LocalDate.of(2017, 7, 4);
		Assert.assertTrue(!newYorkCalendar.isBusinessday(dateToTest));
	}

	/**
	 * Simple tests for LON calendar.
	 */
	@Test
	public void testLondonCalendar() {
		final BusinessdayCalendar londonCalendar = new BusinessdayCalendarExcludingLONHolidays();
		// busDay
		LocalDate dateToTest = LocalDate.of(2017, 6, 12);
		Assert.assertTrue(londonCalendar.isBusinessday(dateToTest));
		// weekend
		dateToTest = LocalDate.of(2017, 6, 10);
		Assert.assertTrue(!londonCalendar.isBusinessday(dateToTest));
		// summer bank holiday
		dateToTest = LocalDate.of(2017, 8, 28);
		Assert.assertTrue(!londonCalendar.isBusinessday(dateToTest));
	}
}
