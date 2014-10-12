/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 07.08.2014
 */

package net.finmath.marketdata.model.curves;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_360;

import org.junit.Test;

/**
 * @author Christian Fries
 *
 */
public class ForwardCurveNelsonSiegelSvenssonTest {

	@Test
	public void test() {

		final double[] nssParameter = new double[] { 0.02, -0.010, 0.16, -0.17, 4.50, 3.5 };

		Calendar referenceDate = new GregorianCalendar(2014, 07, 16);
		String paymentOffsetCode = "3M";
		BusinessdayCalendarInterface paymentBusinessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention = DateRollConvention.MODIFIED_FOLLOWING;
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_360();

		ForwardCurveInterface forwardCurve = new ForwardCurveNelsonSiegelSvensson("EUR Curve", referenceDate, paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, daycountConvention, nssParameter, 365.0/360.0);

		AnalyticModelInterface model = null;		// No model context needed

		assertTrue(Math.abs(forwardCurve.getForward(model,  1.0) - 0.0037364893) < 1E-9);
		assertTrue(Math.abs(forwardCurve.getForward(model,  5.0) - 0.0179295573) < 1E-9);
		assertTrue(Math.abs(forwardCurve.getForward(model, 10.0) - 0.0298339699) < 1E-9);
		assertTrue(Math.abs(forwardCurve.getForward(model, 20.0) - 0.0248458389) < 1E-9);
		assertTrue(Math.abs(forwardCurve.getForward(model, 25.0) - 0.0223256887) < 1E-9);
	}
}
