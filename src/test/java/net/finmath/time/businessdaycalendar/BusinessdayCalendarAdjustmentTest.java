package net.finmath.time.businessdaycalendar;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.time.LocalDate;

import org.junit.Test;

import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;

/** Tests date adjustments and offsets at weekend, month-end and leap-year boundaries. */
public class BusinessdayCalendarAdjustmentTest {

	private final BusinessdayCalendarExcludingWeekends calendar = new BusinessdayCalendarExcludingWeekends();

	@Test
	public void modifiedFollowingStaysWithinTheOriginalMonth() {
		final LocalDate monthEnd = LocalDate.of(2020, 5, 31);
		assertEquals(LocalDate.of(2020, 6, 1), calendar.getAdjustedDate(monthEnd, DateRollConvention.FOLLOWING));
		assertEquals(LocalDate.of(2020, 5, 29), calendar.getAdjustedDate(monthEnd, DateRollConvention.MODIFIED_FOLLOWING));
		assertEquals(LocalDate.of(2020, 7, 20), calendar.getAdjustedDate(LocalDate.of(2020, 7, 18), DateRollConvention.MODIFIED_FOLLOWING));
	}

	@Test
	public void modifiedPrecedingStaysWithinTheOriginalMonth() {
		final LocalDate monthStart = LocalDate.of(2020, 8, 1);
		assertEquals(LocalDate.of(2020, 7, 31), calendar.getAdjustedDate(monthStart, DateRollConvention.PRECEDING));
		assertEquals(LocalDate.of(2020, 8, 3), calendar.getAdjustedDate(monthStart, DateRollConvention.MODIFIED_PRECEDING));
		assertEquals(LocalDate.of(2020, 8, 7), calendar.getAdjustedDate(LocalDate.of(2020, 8, 8), DateRollConvention.MODIFIED_PRECEDING));
	}

	@Test
	public void businessDaysAndUnadjustedWeekendsAreUnchanged() {
		final LocalDate businessDay = LocalDate.of(2020, 6, 1);
		for(final DateRollConvention convention : DateRollConvention.values()) {
			assertEquals(businessDay, calendar.getAdjustedDate(businessDay, convention));
		}
		final LocalDate weekend = LocalDate.of(2020, 5, 31);
		assertEquals(weekend, calendar.getAdjustedDate(weekend, DateRollConvention.UNADJUSTED));
	}

	@Test
	public void signedBusinessDayOffsetsSkipWeekends() {
		assertEquals(LocalDate.of(2020, 6, 1), calendar.getRolledDate(LocalDate.of(2020, 5, 29), 1));
		assertEquals(LocalDate.of(2020, 6, 2), calendar.getRolledDate(LocalDate.of(2020, 5, 29), 2));
		assertEquals(LocalDate.of(2020, 5, 29), calendar.getRolledDate(LocalDate.of(2020, 6, 1), -1));
		assertEquals(LocalDate.of(2020, 5, 28), calendar.getRolledDate(LocalDate.of(2020, 6, 1), -2));
		final LocalDate weekend = LocalDate.of(2020, 5, 30);
		assertEquals(weekend, calendar.getRolledDate(weekend, 0));
	}

	@Test
	public void combinedOffsetsAreAppliedBeforeTheFinalAdjustment() {
		final LocalDate baseDate = LocalDate.of(2020, 1, 31);
		assertEquals(LocalDate.of(2020, 3, 2), calendar.getDateFromDateAndOffsetCode(baseDate, " 1M 2D "));
		assertEquals(LocalDate.of(2020, 3, 2), calendar.getAdjustedDate(baseDate, "1M", DateRollConvention.FOLLOWING));
		assertEquals(LocalDate.of(2020, 2, 28), calendar.getAdjustedDate(baseDate, "1M", DateRollConvention.MODIFIED_FOLLOWING));
		assertEquals(LocalDate.of(2020, 3, 2), calendar.getAdjustedDate(baseDate, "1M 2D", DateRollConvention.FOLLOWING));
	}

	@Test
	public void bulkOffsetsHandleLeapYearsAndBusinessDays() {
		final LocalDate[] expected = {
				LocalDate.of(2020, 2, 1), LocalDate.of(2020, 2, 7), LocalDate.of(2020, 2, 29),
				LocalDate.of(2021, 1, 31), LocalDate.of(2020, 2, 3), LocalDate.of(2020, 3, 2),
				LocalDate.of(2021, 1, 30)
		};
		assertArrayEquals(expected, calendar.createDateFromDateAndOffsetCodes(LocalDate.of(2020, 1, 31),
				new String[] { "1D", "1W", "1M", "1Y", "1BD", "1M 2D", "1" }));
	}
}
