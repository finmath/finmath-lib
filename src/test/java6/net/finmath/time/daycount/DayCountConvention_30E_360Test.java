/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 11.06.215
 */

package net.finmath.time.daycount;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Assert;
import org.junit.Test;

/**
 * Testing the 30E/360 DCC.
 * 
 * @author Christian Fries
 */
public class DayCountConvention_30E_360Test {

	private static Calendar[] startDates = {
		new GregorianCalendar(2007,0,15),
		new GregorianCalendar(2007,0,15),
		new GregorianCalendar(2007,0,15),
		new GregorianCalendar(2007,8,30),
		new GregorianCalendar(2007,8,30),
		new GregorianCalendar(2007,8,30),
		new GregorianCalendar(2007,0,15),
		new GregorianCalendar(2007,0,31),
		new GregorianCalendar(2007,1,28),
		new GregorianCalendar(2006,7,31),
		new GregorianCalendar(2007,1,28),
		new GregorianCalendar(2007,1,14),
		new GregorianCalendar(2007,1,26),
		new GregorianCalendar(2008,1,29),
		new GregorianCalendar(2008,1,29),
		new GregorianCalendar(2008,1,29),
		new GregorianCalendar(2007,1,28),
		new GregorianCalendar(2007,9,31),
		new GregorianCalendar(2007,7,31),
		new GregorianCalendar(2008,1,29),
		new GregorianCalendar(2008,7,31),
		new GregorianCalendar(2009,1,28)
	};

	private static Calendar[] endDates = {
		new GregorianCalendar(2007,0,30),
		new GregorianCalendar(2007,1,15),
		new GregorianCalendar(2007,6,15),
		new GregorianCalendar(2008,2,31),
		new GregorianCalendar(2007,9,31),
		new GregorianCalendar(2008,8,30),
		new GregorianCalendar(2007,0,31),
		new GregorianCalendar(2007,1,28),
		new GregorianCalendar(2007,2,31),
		new GregorianCalendar(2007,1,28),
		new GregorianCalendar(2007,7,31),
		new GregorianCalendar(2007,1,28),
		new GregorianCalendar(2008,1,29),
		new GregorianCalendar(2009,1,28),
		new GregorianCalendar(2008,2,30),
		new GregorianCalendar(2008,2,31),
		new GregorianCalendar(2007,2,05),
		new GregorianCalendar(2007,10,28),
		new GregorianCalendar(2008,1,29),
		new GregorianCalendar(2008,7,31),
		new GregorianCalendar(2009,1,28),
		new GregorianCalendar(2009,7,31)
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
			String message = "Test of daycountFraction("+startDates[i].getTime()+","+endDates[i].getTime()+",30E360)";

			double error = daycount.getDaycountFraction(startDates[i], endDates[i]) - daycountFractions[i];

			Assert.assertEquals(message, 0.0, error, 1E-10);
		}
	}

}
