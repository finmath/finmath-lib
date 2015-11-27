/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */
package net.finmath.tests.time.daycount;

import java.util.ArrayList;

import net.finmath.time.Period;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_30E_360;
import net.finmath.time.daycount.DayCountConvention_ACT_360;
import net.finmath.time.daycount.DayCountConvention_ACT_365;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ICMA;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_YEARFRAC;

import org.joda.time.DateTimeConstants;
import org.joda.time.LocalDate;
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
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2003, DateTimeConstants.NOVEMBER,1), new LocalDate(2004, DateTimeConstants.MAY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.49772438056740776) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(1999,DateTimeConstants.FEBRUARY,1), new LocalDate(1999,DateTimeConstants.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.410958904109589) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(1999,DateTimeConstants.JULY,1), new LocalDate(2000,DateTimeConstants.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 1.0013773486039374) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2002,DateTimeConstants.AUGUST,15), new LocalDate(2003,DateTimeConstants.JULY,15));
		Assert.assertTrue(Math.abs(daycountFraction - 0.915068493150685) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2003,DateTimeConstants.JULY,15), new LocalDate(2004,DateTimeConstants.JANUARY,15));
		Assert.assertTrue(Math.abs(daycountFraction - 0.5040047907777528) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2000,DateTimeConstants.JANUARY,30), new LocalDate(2000,DateTimeConstants.JUNE,30));
		Assert.assertTrue(Math.abs(daycountFraction - 0.41530054644808746) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(1999,DateTimeConstants.NOVEMBER,30), new LocalDate(2000,DateTimeConstants.APRIL,30));
		Assert.assertTrue(Math.abs(daycountFraction - 0.41554008533572875) < 1.0E-8);
	}

	@Test
	public void testDayCountConventionAdditivity_ACT_ACT_ISDA() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_ACT_ISDA();

		// Test additivity of day count fraction
		double daycountFractionTotal = daycountConvention.getDaycountFraction(new LocalDate(2012,DateTimeConstants.MARCH,19), new LocalDate(2013,DateTimeConstants.AUGUST,4));
		double daycountFractionPart1 = daycountConvention.getDaycountFraction(new LocalDate(2012,DateTimeConstants.MARCH,19), new LocalDate(2013,DateTimeConstants.JANUARY,1));
		double daycountFractionPart2 = daycountConvention.getDaycountFraction(new LocalDate(2013,DateTimeConstants.JANUARY,1), new LocalDate(2013,DateTimeConstants.AUGUST,4));
		Assert.assertTrue(Math.abs(daycountFractionTotal - (daycountFractionPart1 + daycountFractionPart2)) < 1.0E-8);
	}
	
	@Test
	public void testDayCountConvention_30E_360() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_30E_360();
		
		double daycountFraction;

		/*
		 * 
		 */
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2003, DateTimeConstants.NOVEMBER,1), new LocalDate(2004,DateTimeConstants.MAY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.5) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(1999,DateTimeConstants.FEBRUARY,1), new LocalDate(1999,DateTimeConstants.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 150.0/360.0) < 1.0E-8);
	}

	@Test
	public void testDayCountConvention_ACT_365() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_365();
		
		double daycountFraction;
		
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2010, DateTimeConstants.MAY, 1), new LocalDate(2011, DateTimeConstants.AUGUST, 31));
		Assert.assertEquals(487.0/365.0, daycountFraction, 1.0E-4);

		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(1999,DateTimeConstants.FEBRUARY,1), new LocalDate(1999,DateTimeConstants.JULY,1));
		Assert.assertEquals(150.0/365.0, daycountFraction, 1.0E-8); 
	}
	
	@Test
	public void testDayCountConvention_ACT_360() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_360();
		
		double daycountFraction;
		
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2010, DateTimeConstants.MAY, 1), new LocalDate(2011, DateTimeConstants.AUGUST, 31));
		Assert.assertEquals(487.0/360.0, daycountFraction, 1.0E-4);

		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(1999,DateTimeConstants.FEBRUARY,1), new LocalDate(1999,DateTimeConstants.JULY,1));
		Assert.assertEquals(150.0/360.0, daycountFraction, 1.0E-8); 
	}
	
	@Test
	public void testDayCountConvention_ACT_ACT_YEARFRAC() {
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_ACT_YEARFRAC();
		
		double daycountFraction;
		
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2010, DateTimeConstants.MAY, 1), new LocalDate(2012, DateTimeConstants.AUGUST, 31));
		Assert.assertEquals(853.0/((365.0+365.0+366.0)/3), daycountFraction, 1.0E-4);

		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(1999,DateTimeConstants.FEBRUARY,1), new LocalDate(1999,DateTimeConstants.JULY,1));
		Assert.assertEquals(150.0/(365.0), daycountFraction, 1.0E-8);
		
		daycountFraction = daycountConvention.getDaycountFraction(new LocalDate(2004,DateTimeConstants.FEBRUARY,1), new LocalDate(2004,DateTimeConstants.JULY,1));
		Assert.assertEquals(151.0/(366.0), daycountFraction, 1.0E-8);
	}

	@Test
	public void testDayCountConventionAdditivity_ACT_ACT_ICMA() {
		ArrayList<Period> periods = new ArrayList<Period>();
		LocalDate start, end;
		for(int i=1980; i<2100; i++) {
			start	= new LocalDate(i, DateTimeConstants.FEBRUARY, 15); 
			end		= new LocalDate(i, DateTimeConstants.MAY, 31);
			periods.add(new Period(start, end, start, end));

			start	= new LocalDate(i, DateTimeConstants.MAY, 15);
			end		= new LocalDate(i, DateTimeConstants.AUGUST, 31);
			periods.add(new Period(start, end, start, end));

			start	= new LocalDate(i, DateTimeConstants.AUGUST, 15);
			end		= new LocalDate(i, DateTimeConstants.NOVEMBER, 30);
			periods.add(new Period(start, end, start, end));

			start	= new LocalDate(i, DateTimeConstants.NOVEMBER, 15);
			end		= new LocalDate(i+1, DateTimeConstants.FEBRUARY, 1);
			end		= end.withDayOfMonth(end.dayOfMonth().getMaximumValue()); // reset the day to end of Feb when it is on a leap year.
			periods.add(new Period(start, end, start, end));
		}
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_ACT_ICMA(periods, 4);

		// Test additivity of day count fraction
		double daycountFractionTotal = daycountConvention.getDaycountFraction(new LocalDate(2012,DateTimeConstants.MARCH,19), new LocalDate(2013,DateTimeConstants.AUGUST,4));
		double daycountFractionPart1 = daycountConvention.getDaycountFraction(new LocalDate(2012,DateTimeConstants.MARCH,19), new LocalDate(2013,DateTimeConstants.JANUARY,1));
		double daycountFractionPart2 = daycountConvention.getDaycountFraction(new LocalDate(2013,DateTimeConstants.JANUARY,1), new LocalDate(2013,DateTimeConstants.AUGUST,4));
		Assert.assertTrue(Math.abs(daycountFractionTotal - (daycountFractionPart1 + daycountFractionPart2)) < 1.0E-8);
	}

	@Test
	public void testDayCountConventionConsistency_ACT_ACT_ICMA_versus_ACT_ACT_ISDA() {
		/*
		 * Quarterly periods for ICMA
		 */
		ArrayList<Period> periods = new ArrayList<Period>();
		for(int i=1980; i<2100; i++) {
			LocalDate start	= new LocalDate(i, DateTimeConstants.JANUARY, 1);
			LocalDate end	= new LocalDate(i, DateTimeConstants.DECEMBER, 31);
			periods.add(new Period(start, end, start, end));
		}
		DayCountConventionInterface daycountConventionICMA = new DayCountConvention_ACT_ACT_ICMA(periods, 1);

		DayCountConventionInterface daycountConventionISDA = new DayCountConvention_ACT_ACT_ISDA();

		LocalDate[] startDates = new LocalDate[] {
				new LocalDate(2003,  DateTimeConstants.NOVEMBER, 1),
				new LocalDate(1999,DateTimeConstants.FEBRUARY,1),
				new LocalDate(1999,DateTimeConstants.JULY,1),
				new LocalDate(2002,DateTimeConstants.AUGUST,15),
				new LocalDate(2003,DateTimeConstants.JULY,15),
				new LocalDate(2000,DateTimeConstants.JANUARY,30),
				new LocalDate(1999,DateTimeConstants.NOVEMBER,30),
				new LocalDate(1999,DateTimeConstants.JANUARY,1),
				new LocalDate(2014,DateTimeConstants.FEBRUARY,1)
		};

		LocalDate[] endDates = new LocalDate[] {
				new LocalDate(2004,DateTimeConstants.MAY,1),
				new LocalDate(1999,DateTimeConstants.JULY,1),
				new LocalDate(2000,DateTimeConstants.JULY,1),
				new LocalDate(2003,DateTimeConstants.JULY,15),
				new LocalDate(2004,DateTimeConstants.JANUARY,15),
				new LocalDate(2000,DateTimeConstants.JUNE,30),
				new LocalDate(2000,DateTimeConstants.APRIL,30),
				new LocalDate(2014,DateTimeConstants.MARCH,1),
				new LocalDate(2014,DateTimeConstants.MARCH,1)
		};

		for(int i=0; i<startDates.length; i++) {
			double daycountFractionICMA = daycountConventionICMA.getDaycount(startDates[i], endDates[i]);
			double daycountFractionISDA = daycountConventionISDA.getDaycount(startDates[i], endDates[i]);
			
			Assert.assertTrue(Math.abs(daycountFractionICMA - daycountFractionISDA) < 1.0E-8);
		}
	}
}
