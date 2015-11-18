/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 11.06.215
 */

package net.finmath.time.daycount;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing the 30E/360 DCC.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30E_360Test {


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

	double[] daycountFractions = new double[] {
			0.0416666667,
			0.0833333333,
			0.5,
			0.5,
			0.0833333333,
			1,
			0.0416666667,
			0.0777777778,
			0.0888888889,
			0.4944444444,
			0.5055555556,
			0.0388888889,
			1.0083333333,
			0.9972222222,
			0.0861111111,
			0.0861111111,
			0.0194444444,
			0.0777777778,
			0.4972222222,
			0.5027777778,
			0.4944444444,
			0.5055555556		
	};

	@Test
	public void test() {
		DayCountConventionInterface daycount = new DayCountConvention_30E_360();
		for(int i=0; i<startDates.length; i++) {
			String message = "Test of daycountFraction("+startDates[i]+","+endDates[i]+",30E360)";

			double error = daycount.getDaycountFraction(startDates[i], endDates[i]) - daycountFractions[i];

			Assert.assertEquals(message, 0.0, error, 1E-10);
		}
	}

}
