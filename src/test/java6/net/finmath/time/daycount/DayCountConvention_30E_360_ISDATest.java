/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 11.06.215
 */

package net.finmath.time.daycount;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import org.junit.Assert;
import org.junit.Test;

/**
 * Testing the 30E/360 DCC.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30E_360_ISDATest {

	private static LocalDate[] startDates = {
		new LocalDate(2007, DateTimeConstants.JANUARY,15),
		new LocalDate(2007, DateTimeConstants.JANUARY,15),
		new LocalDate(2007, DateTimeConstants.JANUARY,15),
		new LocalDate(2007, DateTimeConstants.SEPTEMBER,30),
		new LocalDate(2007, DateTimeConstants.SEPTEMBER,30),
		new LocalDate(2007, DateTimeConstants.SEPTEMBER,30),
		new LocalDate(2007, DateTimeConstants.JANUARY,15),
		new LocalDate(2007, DateTimeConstants.JANUARY,31),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2006, DateTimeConstants.AUGUST,31),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,14),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,26),
		new LocalDate(2008, DateTimeConstants.FEBRUARY,29),
		new LocalDate(2008, DateTimeConstants.FEBRUARY,29),
		new LocalDate(2008, DateTimeConstants.FEBRUARY,29),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2007, DateTimeConstants.OCTOBER,31),
		new LocalDate(2007, DateTimeConstants.AUGUST,31),
		new LocalDate(2008, DateTimeConstants.FEBRUARY,29),
		new LocalDate(2008, DateTimeConstants.AUGUST,31),
		new LocalDate(2009, DateTimeConstants.FEBRUARY,28)
	};

	private static LocalDate[] endDates = {
		new LocalDate(2007, DateTimeConstants.JANUARY,30),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,15),
		new LocalDate(2007, DateTimeConstants.JULY,15),
		new LocalDate(2008, DateTimeConstants.MARCH,31),
		new LocalDate(2007, DateTimeConstants.OCTOBER,31),
		new LocalDate(2008, DateTimeConstants.SEPTEMBER,30),
		new LocalDate(2007, DateTimeConstants.JANUARY,31),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2007, DateTimeConstants.MARCH,31),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2007, DateTimeConstants.AUGUST,31),
		new LocalDate(2007, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2008, DateTimeConstants.FEBRUARY,29),
		new LocalDate(2009, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2008, DateTimeConstants.MARCH,30),
		new LocalDate(2008, DateTimeConstants.MARCH,31),
		new LocalDate(2007, DateTimeConstants.MARCH,05),
		new LocalDate(2007, DateTimeConstants.NOVEMBER,28),
		new LocalDate(2008, DateTimeConstants.FEBRUARY,29),
		new LocalDate(2008, DateTimeConstants.AUGUST,31),
		new LocalDate(2009, DateTimeConstants.FEBRUARY,28),
		new LocalDate(2009, DateTimeConstants.AUGUST,31)
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
		DayCountConventionInterface daycount = new DayCountConvention_30E_360_ISDA(false);
		for(int i=0; i<startDates.length; i++) {
			String message = "Test " + i + " of daycountFraction("+startDates[i]+","+endDates[i]+",30E360)";

			double error = daycount.getDaycountFraction(startDates[i], endDates[i]) - daycountFractionsIfEndDateIsNotTerminationDate[i];

			Assert.assertEquals(message, 0.0, error, 1E-10);
		}
	}

	@Test
	public void testAssumingEndDateIsATerminationDate() {
		DayCountConventionInterface daycount = new DayCountConvention_30E_360_ISDA(true);
		for(int i=0; i<startDates.length; i++) {
			String message = "Test " + i + " of daycountFraction("+startDates[i]+","+endDates[i]+",30E360)";

			double error = daycount.getDaycountFraction(startDates[i], endDates[i]) - daycountFractionsIfEndDateIsTerminationDate[i];

			Assert.assertEquals(message, 0.0, error, 1E-10);
		}
	}
}
