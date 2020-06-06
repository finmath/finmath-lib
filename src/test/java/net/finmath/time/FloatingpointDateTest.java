/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 17.05.2017
 */
package net.finmath.time;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.junit.Assert;
import org.junit.Test;

public class FloatingpointDateTest {

	private static final long SECONDS_PER_DAY = 365*24*60*60;

	/**
	 * Unit test for a roundtrip that the double representation is 1:1 to a LocalDate representation (up to days).
	 */
	@Test
	public void test() {
		final LocalDate referenceDate = LocalDate.of(2016, 1, 1);
		for(int i=0; i<1000; i++) {
			final LocalDate date = referenceDate.plusDays(i);

			final double floatingPointDate = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, date);
			final LocalDate dateFromFloat	= FloatingpointDate.getDateFromFloatingPointDate(referenceDate, floatingPointDate);

			Assert.assertTrue("Roundtrip with date offset of " + i + " days.", dateFromFloat.isEqual(date));
		}
	}

	/**
	 * Unit test comparing the LocalDate function to the LocalDateTime function.
	 */
	@Test
	public void testLocalDateLocalDateTimeConsistency() {
		final LocalDate referenceDate = LocalDate.of(2016, 1, 1);
		for(int i=0; i<1000; i++) {
			final LocalDate date = referenceDate.plusDays(i);

			final double floatingPointDate = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, date);
			final double floatingPointDateTime = FloatingpointDate.getFloatingPointDateFromDate(LocalDateTime.of(referenceDate, LocalTime.of(12, 0)), LocalDateTime.of(date, LocalTime.of(12, 0)));

			Assert.assertEquals("Comparing Date to DateTime", floatingPointDate, floatingPointDateTime, 0.2/SECONDS_PER_DAY);
		}
	}

	/**
	 * Unit test for a roundtrip that the double representation is 1:1 to a LocalDate representation (up to days).
	 */
	@Test
	public void testLocalDateTime() {
		final LocalDateTime referenceDate = LocalDateTime.of(2016, 1, 1,12, 34);
		for(int i=0; i<1000; i+=2) {
			for(int j=0; j<24*60*60; j+=7) {
				final LocalDateTime date = referenceDate.plusDays(i).plusSeconds(j);

				final double floatingPointDate = FloatingpointDate.getFloatingPointDateFromDate(referenceDate, date);
				final LocalDateTime dateFromFloat	= FloatingpointDate.getDateFromFloatingPointDate(referenceDate, floatingPointDate);

				Assert.assertTrue("Roundtrip with date offset of " + i + " days " + j + " seconds.", dateFromFloat.isEqual(date));
			}
		}
	}
}
