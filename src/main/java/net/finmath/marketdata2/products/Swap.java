/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata2.products;

import net.finmath.marketdata2.model.AnalyticModel;
import net.finmath.marketdata2.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata2.model.curves.Curve;
import net.finmath.marketdata2.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata2.model.curves.DiscountCurveInterface;
import net.finmath.marketdata2.model.curves.ForwardCurveInterface;
import net.finmath.stochastic.RandomVariable;
import net.finmath.time.RegularSchedule;
import net.finmath.time.Schedule;
import net.finmath.time.TimeDiscretization;

/**
 * Implements the valuation of a swap using curves (discount curve, forward curve).
 * The swap valuation supports distinct discounting and forward curve.
 * Support for day counting is limited to the capabilities of
 * <code>TimeDiscretization</code>.
 *
 * The swap is just the composition of two <code>SwapLeg</code>s, namely the
 * receiver leg and the payer leg. The value of the swap is the value of the receiver leg minus the value of the payer leg.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class Swap extends AbstractAnalyticProduct implements AnalyticProduct {

	private final AnalyticProduct legReceiver;
	private final AnalyticProduct legPayer;

	/**
	 * Create a swap which values as <code>legReceiver - legPayer</code>.
	 *
	 * @param legReceiver The receiver leg.
	 * @param legPayer The payler leg.
	 */
	public Swap(final AnalyticProduct legReceiver, final AnalyticProduct legPayer) {
		super();
		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}

	/**
	 * Creates a swap with notional exchange. The swap has a unit notional of 1.
	 *
	 * @param scheduleReceiveLeg ScheduleFromPeriods of the receiver leg.
	 * @param forwardCurveReceiveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param schedulePayLeg ScheduleFromPeriods of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadPay Fixed spread on the forward or fix rate.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 * @param isNotionalExchanged If true, both leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public Swap(final Schedule scheduleReceiveLeg,
			final String forwardCurveReceiveName, final double spreadReceive,
			final String discountCurveReceiveName,
			final Schedule schedulePayLeg,
			final String forwardCurvePayName, final double spreadPay,
			final String discountCurvePayName,
			final boolean isNotionalExchanged
			) {
		super();
		legReceiver		= new SwapLeg(scheduleReceiveLeg, forwardCurveReceiveName, spreadReceive, discountCurveReceiveName, isNotionalExchanged /* Notional Exchange */);
		legPayer		= new SwapLeg(schedulePayLeg, forwardCurvePayName, spreadPay, discountCurvePayName, isNotionalExchanged /* Notional Exchange */);
	}

	/**
	 * Creates a swap with notional exchange. The swap has a unit notional of 1.
	 *
	 * @param scheduleReceiveLeg ScheduleFromPeriods of the receiver leg.
	 * @param forwardCurveReceiveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param schedulePayLeg ScheduleFromPeriods of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadPay Fixed spread on the forward or fix rate.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 */
	public Swap(final Schedule scheduleReceiveLeg,
			final String forwardCurveReceiveName, final double spreadReceive,
			final String discountCurveReceiveName,
			final Schedule schedulePayLeg,
			final String forwardCurvePayName, final double spreadPay,
			final String discountCurvePayName
			) {
		this(scheduleReceiveLeg, forwardCurveReceiveName, spreadReceive, discountCurveReceiveName, schedulePayLeg, forwardCurvePayName, spreadPay, discountCurvePayName, true);
	}

	@Override
	public RandomVariable getValue(final double evaluationTime, final AnalyticModel model) {

		final RandomVariable valueReceiverLeg	= legReceiver.getValue(evaluationTime, model);
		final RandomVariable valuePayerLeg	= legPayer.getValue(evaluationTime, model);

		return valueReceiverLeg.sub(valuePayerLeg);
	}

	public static RandomVariable getForwardSwapRate(final TimeDiscretization fixTenor, final TimeDiscretization floatTenor, final ForwardCurveInterface forwardCurve) {
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve);
	}

	public static RandomVariable getForwardSwapRate(final TimeDiscretization fixTenor, final TimeDiscretization floatTenor, final ForwardCurveInterface forwardCurve, final DiscountCurveInterface discountCurve) {
		AnalyticModelFromCurvesAndVols model = null;
		if(discountCurve != null) {
			model			= new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve, discountCurve });
		}
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve, model);
	}

	public static RandomVariable getForwardSwapRate(final Schedule fixSchedule, final Schedule floatSchedule, final ForwardCurveInterface forwardCurve) {
		return getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, null);
	}

	public static RandomVariable getForwardSwapRate(final Schedule fixSchedule, final Schedule floatSchedule, final ForwardCurveInterface forwardCurve, AnalyticModel model) {
		DiscountCurveInterface discountCurve = model == null ? null : model.getDiscountCurve(forwardCurve.getDiscountCurveName());
		if(discountCurve == null) {
			discountCurve	= new DiscountCurveFromForwardCurve(forwardCurve.getName());
			model			= new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve, discountCurve });
		}

		final double evaluationTime = fixSchedule.getFixing(0);	// Consider all values
		final RandomVariable swapAnnuity	= SwapAnnuity.getSwapAnnuity(evaluationTime, fixSchedule, discountCurve, model);

		// Create floating leg
		double fixing			= floatSchedule.getFixing(0);
		double payment			= floatSchedule.getPayment(0);
		double periodLength		= floatSchedule.getPeriodLength(0);

		RandomVariable forward			= forwardCurve.getForward(model, fixing);
		RandomVariable discountFactor	= discountCurve.getDiscountFactor(model, payment);

		RandomVariable floatLeg = forward.mult(discountFactor).mult(periodLength);

		for(int periodIndex=1; periodIndex<floatSchedule.getNumberOfPeriods(); periodIndex++) {
			fixing			= floatSchedule.getFixing(periodIndex);
			payment			= floatSchedule.getPayment(periodIndex);
			periodLength		= floatSchedule.getPeriodLength(periodIndex);

			forward			= forwardCurve.getForward(model, fixing);
			discountFactor	= discountCurve.getDiscountFactor(model, payment);

			floatLeg = floatLeg.add(forward.mult(discountFactor).mult(periodLength));
		}

		final RandomVariable valueFloatLeg = floatLeg.div(discountCurve.getDiscountFactor(model, evaluationTime));

		return valueFloatLeg.div(swapAnnuity);
	}

	/**
	 * Return the receiver leg of the swap, i.e. the leg who's value is added to the swap value.
	 *
	 * @return The receiver leg of the swap.
	 */
	public AnalyticProduct getLegReceiver() {
		return legReceiver;
	}

	/**
	 * Return the payer leg of the swap, i.e. the leg who's value is subtracted from the swap value.
	 *
	 * @return The payer leg of the swap.
	 */
	public AnalyticProduct getLegPayer() {
		return legPayer;
	}

	@Override
	public String toString() {
		return "Swap [legReceiver=" + legReceiver + ", legPayer=" + legPayer
				+ "]";
	}
}
