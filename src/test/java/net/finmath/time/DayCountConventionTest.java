/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.09.2013
 */
package net.finmath.time;

import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConvention_30E_360;
import net.finmath.time.daycount.DayCountConvention_ACT_360;
import net.finmath.time.daycount.DayCountConvention_ACT_365;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ICMA;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_ISDA;
import net.finmath.time.daycount.DayCountConvention_ACT_ACT_YEARFRAC;

/**
 * Unit test for day count convention implementations.
 *
 * @author Christian Fries
 */
public class DayCountConventionTest {

	@Test
	public void testDayCountConvention_ACT_ACT_ISDA() {
		final DayCountConvention daycountConvention = new DayCountConvention_ACT_ACT_ISDA();

		double daycountFraction;

		/*
		 * Standard benchmarks taken from the ISDA documentations.
		 */
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2003, Month.NOVEMBER,1), LocalDate.of(2004, Month.MAY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.49772438056740776) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(1999,Month.FEBRUARY,1), LocalDate.of(1999,Month.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.410958904109589) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(1999,Month.JULY,1), LocalDate.of(2000,Month.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 1.0013773486039374) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2002,Month.AUGUST,15), LocalDate.of(2003,Month.JULY,15));
		Assert.assertTrue(Math.abs(daycountFraction - 0.915068493150685) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2003,Month.JULY,15), LocalDate.of(2004,Month.JANUARY,15));
		Assert.assertTrue(Math.abs(daycountFraction - 0.5040047907777528) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2000,Month.JANUARY,30), LocalDate.of(2000,Month.JUNE,30));
		Assert.assertTrue(Math.abs(daycountFraction - 0.41530054644808746) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(1999,Month.NOVEMBER,30), LocalDate.of(2000,Month.APRIL,30));
		Assert.assertTrue(Math.abs(daycountFraction - 0.41554008533572875) < 1.0E-8);
	}

	@Test
	public void testDayCountConventionAdditivity_ACT_ACT_ISDA() {
		final DayCountConvention daycountConvention = new DayCountConvention_ACT_ACT_ISDA();

		// Test additivity of day count fraction
		final double daycountFractionTotal = daycountConvention.getDaycountFraction(LocalDate.of(2012,Month.MARCH,19), LocalDate.of(2013,Month.AUGUST,4));
		final double daycountFractionPart1 = daycountConvention.getDaycountFraction(LocalDate.of(2012,Month.MARCH,19), LocalDate.of(2013,Month.JANUARY,1));
		final double daycountFractionPart2 = daycountConvention.getDaycountFraction(LocalDate.of(2013,Month.JANUARY,1), LocalDate.of(2013,Month.AUGUST,4));
		Assert.assertTrue(Math.abs(daycountFractionTotal - (daycountFractionPart1 + daycountFractionPart2)) < 1.0E-8);
	}

	@Test
	public void testDayCountConvention_30E_360() {
		final DayCountConvention daycountConvention = new DayCountConvention_30E_360();

		double daycountFraction;

		/*
		 *
		 */
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2003, Month.NOVEMBER,1), LocalDate.of(2004,Month.MAY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 0.5) < 1.0E-8);

		//
		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(1999,Month.FEBRUARY,1), LocalDate.of(1999,Month.JULY,1));
		Assert.assertTrue(Math.abs(daycountFraction - 150.0/360.0) < 1.0E-8);
	}

	@Test
	public void testDayCountConvention_ACT_365() {
		final DayCountConvention daycountConvention = new DayCountConvention_ACT_365();

		double daycountFraction;

		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2010, Month.MAY, 1), LocalDate.of(2011, Month.AUGUST, 31));
		Assert.assertEquals(487.0/365.0, daycountFraction, 1.0E-4);

		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(1999,Month.FEBRUARY,1), LocalDate.of(1999,Month.JULY,1));
		Assert.assertEquals(150.0/365.0, daycountFraction, 1.0E-8);
	}

	@Test
	public void testDayCountConvention_ACT_360() {
		final DayCountConvention daycountConvention = new DayCountConvention_ACT_360();

		double daycountFraction;

		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2010, Month.MAY, 1), LocalDate.of(2011, Month.AUGUST, 31));
		Assert.assertEquals(487.0/360.0, daycountFraction, 1.0E-4);

		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(1999,Month.FEBRUARY,1), LocalDate.of(1999,Month.JULY,1));
		Assert.assertEquals(150.0/360.0, daycountFraction, 1.0E-8);
	}

	@Test
	public void testDayCountConvention_ACT_ACT_YEARFRAC() {
		final DayCountConvention daycountConvention = new DayCountConvention_ACT_ACT_YEARFRAC();

		double daycountFraction;

		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2010, Month.MAY, 1), LocalDate.of(2012, Month.AUGUST, 31));
		Assert.assertEquals(853.0/((365.0+365.0+366.0)/3), daycountFraction, 1.0E-4);

		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(1999,Month.FEBRUARY,1), LocalDate.of(1999,Month.JULY,1));
		Assert.assertEquals(150.0/(365.0), daycountFraction, 1.0E-8);

		daycountFraction = daycountConvention.getDaycountFraction(LocalDate.of(2004,Month.FEBRUARY,1), LocalDate.of(2004,Month.JULY,1));
		Assert.assertEquals(151.0/(366.0), daycountFraction, 1.0E-8);
	}

	@Test
	public void testDayCountConventionAdditivity_ACT_ACT_ICMA() {
		final ArrayList<Period> periods = new ArrayList<>();
		LocalDate start, end;
		for(int i=1980; i<2100; i++) {
			start	= LocalDate.of(i, Month.FEBRUARY, 15);
			end		= LocalDate.of(i, Month.FEBRUARY.plus(3), 31);
			periods.add(new Period(start, end, start, end));

			start	= LocalDate.of(i, Month.FEBRUARY.plus(3), 15);
			end		= LocalDate.of(i, Month.FEBRUARY.plus(6), 31);
			periods.add(new Period(start, end, start, end));

			start	= LocalDate.of(i, Month.FEBRUARY.plus(6), 15);
			end		= LocalDate.of(i, Month.FEBRUARY.plus(9), 30);
			periods.add(new Period(start, end, start, end));

			start	= LocalDate.of(i, Month.FEBRUARY.plus(9), 15);
			end		= LocalDate.of(i+1, Month.FEBRUARY, 1);
			end		= end.withDayOfMonth(end.lengthOfMonth()); // reset the day to end of Feb when it is on a leap year.
			periods.add(new Period(start, end, start, end));
		}
		final DayCountConvention daycountConvention = new DayCountConvention_ACT_ACT_ICMA(periods, 4);

		// Test additivity of day count fraction
		final double daycountFractionTotal = daycountConvention.getDaycountFraction(LocalDate.of(2012,Month.MARCH,19), LocalDate.of(2013,Month.AUGUST,4));
		final double daycountFractionPart1 = daycountConvention.getDaycountFraction(LocalDate.of(2012,Month.MARCH,19), LocalDate.of(2013,Month.JANUARY,1));
		final double daycountFractionPart2 = daycountConvention.getDaycountFraction(LocalDate.of(2013,Month.JANUARY,1), LocalDate.of(2013,Month.AUGUST,4));
		Assert.assertTrue(Math.abs(daycountFractionTotal - (daycountFractionPart1 + daycountFractionPart2)) < 1.0E-8);
	}

	@Test
	public void testDayCountConventionConsistency_ACT_ACT_ICMA_versus_ACT_ACT_ISDA() {
		/*
		 * Quarterly periods for ICMA
		 */
		final ArrayList<Period> periods = new ArrayList<>();
		for(int i=1980; i<2100; i++) {
			final LocalDate start	= LocalDate.of(i, Month.JANUARY, 1);
			final LocalDate end	= LocalDate.of(i, Month.DECEMBER, 31);
			periods.add(new Period(start, end, start, end));
		}
		final DayCountConvention daycountConventionICMA = new DayCountConvention_ACT_ACT_ICMA(periods, 1);

		final DayCountConvention daycountConventionISDA = new DayCountConvention_ACT_ACT_ISDA();

		final LocalDate[] startDates = new LocalDate[] {
				LocalDate.of(2003,  Month.NOVEMBER, 1),
				LocalDate.of(1999,Month.FEBRUARY,1),
				LocalDate.of(1999,Month.JULY,1),
				LocalDate.of(2002,Month.AUGUST,15),
				LocalDate.of(2003,Month.JULY,15),
				LocalDate.of(2000,Month.JANUARY,30),
				LocalDate.of(1999,Month.NOVEMBER,30),
				LocalDate.of(1999,Month.JANUARY,1),
				LocalDate.of(2014,Month.FEBRUARY,1)
		};

		final LocalDate[] endDates = new LocalDate[] {
				LocalDate.of(2004,Month.MAY,1),
				LocalDate.of(1999,Month.JULY,1),
				LocalDate.of(2000,Month.JULY,1),
				LocalDate.of(2003,Month.JULY,15),
				LocalDate.of(2004,Month.JANUARY,15),
				LocalDate.of(2000,Month.JUNE,30),
				LocalDate.of(2000,Month.APRIL,30),
				LocalDate.of(2014,Month.MARCH,1),
				LocalDate.of(2014,Month.MARCH,1)
		};

		for(int i=0; i<startDates.length; i++) {
			final double daycountFractionICMA = daycountConventionICMA.getDaycount(startDates[i], endDates[i]);
			final double daycountFractionISDA = daycountConventionISDA.getDaycount(startDates[i], endDates[i]);

			Assert.assertTrue(Math.abs(daycountFractionICMA - daycountFractionISDA) < 1.0E-8);
		}
	}
}
