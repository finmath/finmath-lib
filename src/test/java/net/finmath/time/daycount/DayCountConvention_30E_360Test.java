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
public class DayCountConvention_30E_360Test {

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
		final DayCountConvention daycount = new DayCountConvention_30E_360();
		for(int i=0; i<startDates.length; i++) {
			final String message = "Test of daycountFraction("+startDates[i]+","+endDates[i]+",30E360)";

			final double error = daycount.getDaycountFraction(startDates[i], endDates[i]) - daycountFractions[i];

			Assert.assertEquals(message, 0.0, error, 1E-10);
		}
	}

}
