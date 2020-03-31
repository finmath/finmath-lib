/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 11.06.215
 */

package net.finmath.time.daycount;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing the 30E/360 DCC.
 *
 * @author Christian Fries
 */
public class DayCountConvention_30E_360_ISDATest {

	private static LocalDate[] startDates = {
			LocalDate.of(2007,1,15),
			LocalDate.of(2007,1,15),
			LocalDate.of(2007,1,15),
			LocalDate.of(2007,9,30),
			LocalDate.of(2007,9,30),
			LocalDate.of(2007,9,30),
			LocalDate.of(2007,1,15),
			LocalDate.of(2007,1,31),
			LocalDate.of(2007,2,28),
			LocalDate.of(2006,8,31),
			LocalDate.of(2007,2,28),
			LocalDate.of(2007,2,14),
			LocalDate.of(2007,2,26),
			LocalDate.of(2008,2,29),
			LocalDate.of(2008,2,29),
			LocalDate.of(2008,2,29),
			LocalDate.of(2007,2,28),
			LocalDate.of(2007,10,31),
			LocalDate.of(2007,8,31),
			LocalDate.of(2008,2,29),
			LocalDate.of(2008,8,31),
			LocalDate.of(2009,2,28)
	};

	private static LocalDate[] endDates = {
			LocalDate.of(2007,1,30),
			LocalDate.of(2007,2,15),
			LocalDate.of(2007,7,15),
			LocalDate.of(2008,3,31),
			LocalDate.of(2007,10,31),
			LocalDate.of(2008,9,30),
			LocalDate.of(2007,1,31),
			LocalDate.of(2007,2,28),
			LocalDate.of(2007,3,31),
			LocalDate.of(2007,2,28),
			LocalDate.of(2007,8,31),
			LocalDate.of(2007,2,28),
			LocalDate.of(2008,2,29),
			LocalDate.of(2009,2,28),
			LocalDate.of(2008,3,30),
			LocalDate.of(2008,3,31),
			LocalDate.of(2007,3, 5),
			LocalDate.of(2007,11,28),
			LocalDate.of(2008,2,29),
			LocalDate.of(2008,8,31),
			LocalDate.of(2009,2,28),
			LocalDate.of(2009,8,31)
	};

	double[] daycountFractionsIfEndDateIsNotTerminationDate = new double[] {
			0.0416666667,
			0.0833333333,
			0.5,
			0.5,
			0.0833333333,
			1,
			0.0416666667,
			0.0833333333,
			0.0833333333,
			0.5,
			0.5,
			0.0444444444,
			1.0111111111,
			1.0, /* value in case endDate is not a termination date, otherwise 0.9944444444 */
			0.0833333333,
			0.0833333333,
			0.0138888889,
			0.0777777778,
			0.5, /* value in case endDate is not a termination date, otherwise 0.4944444444 */
			0.5,
			0.5, /* value in case endDate is not a termination date, otherwise 0.4944444444 */
			0.5
	};

	double[] daycountFractionsIfEndDateIsTerminationDate = new double[] {
			0.0416666667,
			0.0833333333,
			0.5,
			0.5,
			0.0833333333,
			1,
			0.0416666667,
			0.0833333333-2.0/360.0,
			0.0833333333,
			0.5-2.0/360.0,
			0.5,
			0.0444444444-2.0/360.0,
			1.0111111111-1.0/360.0,
			0.9944444444, /* value in case endDate is a termination date, otherwise 1.0 */
			0.0833333333,
			0.0833333333,
			0.0138888889,
			0.0777777778,
			0.5-1.0/360.0, /* value in case endDate is a termination date, otherwise 0.5 */
			0.5,
			0.4944444444, /* value in case endDate is a termination date, otherwise 0.5 */
			0.5
	};

	@Test
	public void testAssumingEndDateIsNotATerminationDate() {
		final DayCountConvention daycount = new DayCountConvention_30E_360_ISDA(false);
		for(int i=0; i<startDates.length; i++) {
			final String message = "Test " + i + " of daycountFraction("+startDates[i]+","+endDates[i]+",30E360)";

			final double error = daycount.getDaycountFraction(startDates[i], endDates[i]) - daycountFractionsIfEndDateIsNotTerminationDate[i];

			Assert.assertEquals(message, 0.0, error, 1E-10);
		}
	}

	@Test
	public void testAssumingEndDateIsATerminationDate() {
		final DayCountConvention daycount = new DayCountConvention_30E_360_ISDA(true);
		for(int i=0; i<startDates.length; i++) {
			final String message = "Test " + i + " of daycountFraction("+startDates[i]+","+endDates[i]+",30E360)";

			final double error = daycount.getDaycountFraction(startDates[i], endDates[i]) - daycountFractionsIfEndDateIsTerminationDate[i];

			Assert.assertEquals(message, 0.0, error, 1E-10);
		}
	}
}
