/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 07.08.2014
 */

package net.finmath.marketdata.model.curves;

import org.junit.Assert;
import org.junit.Test;
import org.threeten.bp.LocalDate;
import org.threeten.bp.Month;

import net.finmath.marketdata.model.AnalyticModelInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarExcludingTARGETHolidays;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarInterface.DateRollConvention;
import net.finmath.time.daycount.DayCountConventionInterface;
import net.finmath.time.daycount.DayCountConvention_ACT_360;

/**
 * @author Christian Fries
 *
 */
public class ForwardCurveNelsonSiegelSvenssonTest {

	@Test
	public void test() {

		final double[] nssParameters = new double[] { 0.02, -0.010, 0.16, -0.17, 4.50, 3.5 };

		LocalDate referenceDate = LocalDate.of(2014, Month.AUGUST, 16);
		String paymentOffsetCode = "3M";
		BusinessdayCalendarInterface paymentBusinessdayCalendar = new BusinessdayCalendarExcludingTARGETHolidays();
		BusinessdayCalendarInterface.DateRollConvention paymentDateRollConvention = DateRollConvention.MODIFIED_FOLLOWING;
		DayCountConventionInterface daycountConvention = new DayCountConvention_ACT_360();

		ForwardCurveInterface forwardCurve = new ForwardCurveNelsonSiegelSvensson("EUR Curve", referenceDate, paymentOffsetCode, paymentBusinessdayCalendar, paymentDateRollConvention, daycountConvention, nssParameters, 365.0/360.0, 0.0);

		AnalyticModelInterface model = null;		// No model context needed

		Assert.assertEquals("Forward", 0.0037364893, forwardCurve.getForward(model,  1.0), 1E-9);
		Assert.assertEquals("Forward", 0.0179295573, forwardCurve.getForward(model,  5.0), 1E-9);
		Assert.assertEquals("Forward", 0.0298339699, forwardCurve.getForward(model, 10.0), 1E-9);
		Assert.assertEquals("Forward", 0.0248458389, forwardCurve.getForward(model, 20.0), 1E-9);
		Assert.assertEquals("Forward", 0.0223256887, forwardCurve.getForward(model, 25.0), 1E-9);
	}
}
