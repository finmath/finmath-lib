package net.finmath.marketdata.products;

import java.time.LocalDate;

import org.junit.Assert;
import org.junit.Test;

import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurveInterpolation;
import net.finmath.time.Schedule;
import net.finmath.time.ScheduleGenerator;
import net.finmath.time.ScheduleGenerator.ShortPeriodConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendar.DateRollConvention;
import net.finmath.time.businessdaycalendar.BusinessdayCalendarAny;

public class DepositTest {

	private final String dcName = "discountCurve";
	private final double[] times = { 0., 1. };
	private final double[] dcFactors = { 1., 1. / 1.05 };
	private final DiscountCurveInterpolation dc;
	private final AnalyticModelFromCurvesAndVols model;
	private final Schedule sched;
	private final double accuracy = 1e-10;

	public DepositTest() {
		dc = DiscountCurveInterpolation.createDiscountCurveFromDiscountFactors(dcName, times, dcFactors);

		// set up model
		model = new AnalyticModelFromCurvesAndVols(new Curve[] { dc });

		// set up deposit
		final LocalDate refDate = LocalDate.of(2017, 1, 1);
		final LocalDate startDate = refDate.plusDays(2);
		final LocalDate maturity = startDate.plusDays(180);
		sched = ScheduleGenerator.createScheduleFromConventions(refDate, startDate, maturity,
				ScheduleGenerator.Frequency.ANNUAL, ScheduleGenerator.DaycountConvention.ACT_360,
				ShortPeriodConvention.FIRST, DateRollConvention.UNADJUSTED,
				new BusinessdayCalendarAny(), -2, 0);

	}

	/**
	 * Tests that the functions getRate and getValue are consistent
	 */
	@Test
	public void testRateValueConsistency() {

		// set up initial deposit
		Deposit depo = new Deposit(sched, 0.01, dcName);

		// determine market rate for this deposit
		final double rate = depo.getRate(model);

		// setup depo as market deposit
		depo = new Deposit(sched, rate, dcName);

		// check that value of depo is zero
		Assert.assertEquals("Price deviation", 0., depo.getValue(model), accuracy);

	}

	/**
	 * Test deposit valuation by comparing it to the corresponding cashflows
	 */
	@Test
	public void testDepositValue() {

		final double rate = 0.1; // deliberately large interest rate

		// set up deposit
		final Deposit depo = new Deposit(sched, rate, dcName);

		// set up corresponding cashflows
		final Cashflow payout = new Cashflow("EUR", 1, sched.getPeriodStart(0), true, dcName);
		final Cashflow payback = new Cashflow("EUR", 1 + rate * sched.getPeriodLength(0),
				sched.getPeriodEnd(0), false, dcName);

		Assert.assertEquals("Value deviation between depo and its cashflows", 0.,
				depo.getValue(model) - payout.getValue(model) - payback.getValue(model), accuracy);
	}

	/**
	 * Test correct usage of evaluation time
	 */
	@Test
	public void testEvaluationTime() {

		// set up deposit
		Deposit depo = new Deposit(sched, 0.1, dcName);
		final double rate = depo.getRate(model);
		depo =  new Deposit(sched, rate, dcName);

		// deposit has been fixed at market conditions => value 0
		Assert.assertEquals("Value today", 0.,
				depo.getValue(0., model), accuracy);
		// after maturity deposit has value 0
		Assert.assertEquals("Value after maturity", 0.,
				depo.getValue(1., model), accuracy);
		// after payout only the payback is expected and rate > 0 => value > 0
		Assert.assertTrue("Value after payout must be positive",
				depo.getValue(5 / 360., model)>0.);

	}
}
