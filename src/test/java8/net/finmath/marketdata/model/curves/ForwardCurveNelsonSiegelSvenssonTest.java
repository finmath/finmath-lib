/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.08.2014
 */

package net.finmath.marketdata.model.curves;

import java.time.LocalDate;
import java.time.Month;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.daycount.DayCountConvention;
import net.finmath.time.daycount.DayCountConvention_ACT_360;

/**
 * @author Christian Fries
 *
 */
public class ForwardCurveNelsonSiegelSvenssonTest {

	@Test
	public void test() {

		final double[] nssParameters = new double[] { 0.02, -0.010, 0.16, -0.17, 4.50, 3.5 };

		final LocalDate referenceDate = LocalDate.of(2014, Month.AUGUST, 16);
		final String paymentOffsetCode = "3M";
		final BusinessdayCalendar paymentBusinessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		final BusinessdayCalendar.DateRollConvention paymentDateRollConvention = DateRollConvention.MODIFIED_FOLLOWING;
		final DayCountConvention daycountConvention = new DayCountConvention_ACT_360();

		final ForwardCurve forwardCurve = new ForwardCurveNelsonSiegelSvensson("EUR CurveFromInterpolationPoints", referenceDate, paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, daycountConvention, nssParameters, 365.0/360.0, 0.0);

		final AnalyticModel model = null;		// No model context needed

		Assert.assertEquals("Forward", 0.0037364893, forwardCurve.getForward(model,  1.0), 1E-9);
		Assert.assertEquals("Forward", 0.0179295573, forwardCurve.getForward(model,  5.0), 1E-9);
		Assert.assertEquals("Forward", 0.0298339699, forwardCurve.getForward(model, 10.0), 1E-9);
		Assert.assertEquals("Forward", 0.0248458389, forwardCurve.getForward(model, 20.0), 1E-9);
		Assert.assertEquals("Forward", 0.0223256887, forwardCurve.getForward(model, 25.0), 1E-9);
	}
}
