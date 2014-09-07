/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */
package net.finmath.tests.time.daycount;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

import net.finmath.time.Period;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_30E_360;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ICMA;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit test for day count convention implementations.
 * 
 * @author Christian Fries
 */
public class DayCountConventionTest {

	@Test
	public void testDayCountConvention_ACT_ACT_ISDA() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_ACT_ISDA();
		
		double daycountFraction;

		/*
		 * Standard benchmarks taken from the ISDA documentations.
		 */
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2003,Calendar.NOVEMBER,1), new GregorianCalendar(2004,Calendar.MAY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.49772438056740776) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(1999,Calendar.FEBRUARY,1), new GregorianCalendar(1999,Calendar.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.410958904109589) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(1999,Calendar.JULY,1), new GregorianCalendar(2000,Calendar.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 1.0013773486039374) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2002,Calendar.AUGUST,15), new GregorianCalendar(2003,Calendar.JULY,15));
		Assert.assertTrue(Math.abs(daycountFraction - 0.915068493150685) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2003,Calendar.JULY,15), new GregorianCalendar(2004,Calendar.JANUARY,15));
		Assert.assertTrue(Math.abs(daycountFraction - 0.5040047907777528) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2000,Calendar.JANUARY,30), new GregorianCalendar(2000,Calendar.JUNE,30));
		Assert.assertTrue(Math.abs(daycountFraction - 0.41530054644808746) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(1999,Calendar.NOVEMBER,30), new GregorianCalendar(2000,Calendar.APRIL,30));
		Assert.assertTrue(Math.abs(daycountFraction - 0.41554008533572875) < 1.0E-8);
	}

	@Test
	public void testDayCountConventionAdditivity_ACT_ACT_ISDA() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_ACT_ISDA();

		// Test additivity of day count fraction
		double daycountFractionTotal = daycountConvention.getDaycountFraction(new GregorianCalendar(2012,Calendar.MARCH,19), new GregorianCalendar(2013,Calendar.AUGUST,4));
		double daycountFractionPart1 = daycountConvention.getDaycountFraction(new GregorianCalendar(2012,Calendar.MARCH,19), new GregorianCalendar(2013,Calendar.JANUARY,1));
		double daycountFractionPart2 = daycountConvention.getDaycountFraction(new GregorianCalendar(2013,Calendar.JANUARY,1), new GregorianCalendar(2013,Calendar.AUGUST,4));
		Assert.assertTrue(Math.abs(daycountFractionTotal - (daycountFractionPart1 + daycountFractionPart2)) < 1.0E-8);
	}
	
	@Test
	public void testDayCountConvention_30E_360() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_30E_360();
		
		double daycountFraction;

		/*
		 * 
		 */
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(2003,Calendar.NOVEMBER,1), new GregorianCalendar(2004,Calendar.MAY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.5) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new GregorianCalendar(1999,Calendar.FEBRUARY,1), new GregorianCalendar(1999,Calendar.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 150.0/360.0) < 1.0E-8);
	}

	@Test
	public void testDayCountConventionAdditivity_ACT_ACT_ICMA() {
		ArrayList<Period> periods = new ArrayList<Period>();
		Calendar start, end;
		for(int i=1980; i<2100; i++) {
			start	= new GregorianCalendar(i, Calendar.FEBRUARY   , 15);
			end		= new GregorianCalendar(i, Calendar.FEBRUARY +3, 31);
			periods.add(new Period(start, end, start, end));

			start	= new GregorianCalendar(i, Calendar.FEBRUARY +3  , 15);
			end		= new GregorianCalendar(i, Calendar.FEBRUARY +6, 31);
			periods.add(new Period(start, end, start, end));

			start	= new GregorianCalendar(i, Calendar.FEBRUARY +6  , 15);
			end		= new GregorianCalendar(i, Calendar.FEBRUARY +9, 31);
			periods.add(new Period(start, end, start, end));

			start	= new GregorianCalendar(i, Calendar.FEBRUARY +9  , 15);
			end		= new GregorianCalendar(i+1, Calendar.FEBRUARY +0, 31);
			periods.add(new Period(start, end, start, end));
		}
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_ACT_ICMA(periods, 4);

		// Test additivity of day count fraction
		double daycountFractionTotal = daycountConvention.getDaycountFraction(new GregorianCalendar(2012,Calendar.MARCH,19), new GregorianCalendar(2013,Calendar.AUGUST,4));
		double daycountFractionPart1 = daycountConvention.getDaycountFraction(new GregorianCalendar(2012,Calendar.MARCH,19), new GregorianCalendar(2013,Calendar.JANUARY,1));
		double daycountFractionPart2 = daycountConvention.getDaycountFraction(new GregorianCalendar(2013,Calendar.JANUARY,1), new GregorianCalendar(2013,Calendar.AUGUST,4));
		Assert.assertTrue(Math.abs(daycountFractionTotal - (daycountFractionPart1 + daycountFractionPart2)) < 1.0E-8);
	}

	@Test
	public void testDayCountConventionConsistency_ACT_ACT_ICMA_versus_ACT_ACT_ISDA() {
		/*
		 * Quarterly periods for ICMA
		 */
		ArrayList<Period> periods = new ArrayList<Period>();
		for(int i=1980; i<2100; i++) {
			Calendar start	= new GregorianCalendar(i, Calendar.JANUARY, 1);
			Calendar end	= new GregorianCalendar(i, Calendar.DECEMBER, 31);
			periods.add(new Period(start, end, start, end));
		}
		DayCountConventionInterface daycountConventionICMA = new DayCountConvention_ACT_ACT_ICMA(periods, 1);

		DayCountConventionInterface daycountConventionISDA = new DayCountConvention_ACT_ACT_ISDA();

		Calendar[] startDates = new GregorianCalendar[] {
				new GregorianCalendar(2003,Calendar.NOVEMBER,1),
				new GregorianCalendar(1999,Calendar.FEBRUARY,1),
				new GregorianCalendar(1999,Calendar.JULY,1),
				new GregorianCalendar(2002,Calendar.AUGUST,15),
				new GregorianCalendar(2003,Calendar.JULY,15),
				new GregorianCalendar(2000,Calendar.JANUARY,30),
				new GregorianCalendar(1999,Calendar.NOVEMBER,30),
				new GregorianCalendar(1999,Calendar.JANUARY,1),
				new GregorianCalendar(2014,Calendar.FEBRUARY,1)
		};

		Calendar[] endDates = new GregorianCalendar[] {
				new GregorianCalendar(2004,Calendar.MAY,1),
				new GregorianCalendar(1999,Calendar.JULY,1),
				new GregorianCalendar(2000,Calendar.JULY,1),
				new GregorianCalendar(2003,Calendar.JULY,15),
				new GregorianCalendar(2004,Calendar.JANUARY,15),
				new GregorianCalendar(2000,Calendar.JUNE,30),
				new GregorianCalendar(2000,Calendar.APRIL,30),
				new GregorianCalendar(2014,Calendar.MARCH,1),
				new GregorianCalendar(2014,Calendar.MARCH,1)
		};

		for(int i=0; i<startDates.length; i++) {
			double daycountFractionICMA = daycountConventionICMA.getDaycount(startDates[i], endDates[i]);
			double daycountFractionISDA = daycountConventionISDA.getDaycount(startDates[i], endDates[i]);
			
			Assert.assertTrue(Math.abs(daycountFractionICMA - daycountFractionISDA) < 1.0E-8);
		}
	}
}
