/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */
package net.finmath.tests.time.daycount;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;

import org.junit.Test;

/**
 * @author Christian Fries
 * 
 */
public class DayCountConventionTest {

	@Test
	public void testDayCountConvention_ACT_ACT_ISDA() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_ACT_ISDA();
		
		double daycountFraction;

		/*
		 * 
		 */
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2003,Calendar.NOVEMBER,1), new GregorianCalendar(2004,Calendar.MAY,1));
		assertTrue(Math.abs(daycountFraction - 0.49772438056740776) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(1999,Calendar.FEBRUARY,1), new GregorianCalendar(1999,Calendar.JULY,1));
		assertTrue(Math.abs(daycountFraction - 0.410958904109589) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(1999,Calendar.JULY,1), new GregorianCalendar(2000,Calendar.JULY,1));
		assertTrue(Math.abs(daycountFraction - 1.0013773486039374) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2002,Calendar.AUGUST,15), new GregorianCalendar(2003,Calendar.JULY,15));
		assertTrue(Math.abs(daycountFraction - 0.915068493150685) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2003,Calendar.JULY,15), new GregorianCalendar(2004,Calendar.JANUARY,15));
		assertTrue(Math.abs(daycountFraction - 0.5040047907777528) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2000,Calendar.JANUARY,30), new GregorianCalendar(2000,Calendar.JUNE,30));
		assertTrue(Math.abs(daycountFraction - 0.41530054644808746) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(1999,Calendar.NOVEMBER,30), new GregorianCalendar(2000,Calendar.APRIL,30));
		assertTrue(Math.abs(daycountFraction - 0.41554008533572875) < 1.0E-8);
	}
}
