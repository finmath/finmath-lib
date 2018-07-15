/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.analytic.products;

import net.finmath.analytic.model.AnalyticModel;
import net.finmath.analytic.model.AnalyticModelInterface;
import net.finmath.analytic.model.curves.CurveInterface;
import net.finmath.analytic.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.analytic.model.curves.DiscountCurveInterface;
import net.finmath.analytic.model.curves.ForwardCurveInterface;
import net.finmath.stochastic.RandomVariableInterface;
import net.finmath.time.RegularSchedule;
import net.finmath.time.ScheduleInterface;
import net.finmath.time.TimeDiscretizationInterface;

/**
 * Implements the valuation of a swap using curves (discount curve, forward curve).
 * The swap valuation supports distinct discounting and forward curve.
 * Support for day counting is limited to the capabilities of
 * <code>TimeDiscretizationInterface</code>.
 *
 * The swap is just the composition of two <code>SwapLeg</code>s, namely the
 * receiver leg and the payer leg. The value of the swap is the value of the receiver leg minus the value of the payer leg.
 *
 * @author Christian Fries
 */
public class Swap extends AbstractAnalyticProduct implements AnalyticProductInterface {

	private final AnalyticProductInterface legReceiver;
	private final AnalyticProductInterface legPayer;

	/**
	 * Create a swap which values as <code>legReceiver - legPayer</code>.
	 *
	 * @param legReceiver The receiver leg.
	 * @param legPayer The payler leg.
	 */
	public Swap(AnalyticProductInterface legReceiver, AnalyticProductInterface legPayer) {
		super();
		this.legReceiver = legReceiver;
		this.legPayer = legPayer;
	}

	/**
	 * Creates a swap with notional exchange. The swap has a unit notional of 1.
	 *
	 * @param scheduleReceiveLeg Schedule of the receiver leg.
	 * @param forwardCurveReceiveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param schedulePayLeg Schedule of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadPay Fixed spread on the forward or fix rate.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 * @param isNotionalExchanged If true, both leg will pay notional at the beginning of each swap period and receive notional at the end of the swap period. Note that the cash flow date for the notional is periodStart and periodEnd (not fixingDate and paymentDate).
	 */
	public Swap(ScheduleInterface scheduleReceiveLeg,
			String forwardCurveReceiveName, double spreadReceive,
			String discountCurveReceiveName,
			ScheduleInterface schedulePayLeg,
			String forwardCurvePayName, double spreadPay,
			String discountCurvePayName,
			boolean isNotionalExchanged
			) {
		super();
		legReceiver		= new SwapLeg(scheduleReceiveLeg, forwardCurveReceiveName, spreadReceive, discountCurveReceiveName, isNotionalExchanged /* Notional Exchange */);
		legPayer		= new SwapLeg(schedulePayLeg, forwardCurvePayName, spreadPay, discountCurvePayName, isNotionalExchanged /* Notional Exchange */);
	}

	/**
	 * Creates a swap with notional exchange. The swap has a unit notional of 1.
	 *
	 * @param scheduleReceiveLeg Schedule of the receiver leg.
	 * @param forwardCurveReceiveName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param schedulePayLeg Schedule of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param spreadPay Fixed spread on the forward or fix rate.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 */
	public Swap(ScheduleInterface scheduleReceiveLeg,
			String forwardCurveReceiveName, double spreadReceive,
			String discountCurveReceiveName,
			ScheduleInterface schedulePayLeg,
			String forwardCurvePayName, double spreadPay,
			String discountCurvePayName
			) {
		this(scheduleReceiveLeg, forwardCurveReceiveName, spreadReceive, discountCurveReceiveName, schedulePayLeg, forwardCurvePayName, spreadPay, discountCurvePayName, true);
	}

	@Override
	public RandomVariableInterface getValue(double evaluationTime, AnalyticModelInterface model) {

		RandomVariableInterface valueReceiverLeg	= legReceiver.getValue(evaluationTime, model);
		RandomVariableInterface valuePayerLeg	= legPayer.getValue(evaluationTime, model);

		return valueReceiverLeg.sub(valuePayerLeg);
	}

	public static RandomVariableInterface getForwardSwapRate(TimeDiscretizationInterface fixTenor, TimeDiscretizationInterface floatTenor, ForwardCurveInterface forwardCurve) {
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve);
	}

	public static RandomVariableInterface getForwardSwapRate(TimeDiscretizationInterface fixTenor, TimeDiscretizationInterface floatTenor, ForwardCurveInterface forwardCurve, DiscountCurveInterface discountCurve) {
		AnalyticModel model = null;
		if(discountCurve != null) {
			model			= new AnalyticModel(new CurveInterface[] { forwardCurve, discountCurve });
		}
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve, model);
	}

	public static RandomVariableInterface getForwardSwapRate(ScheduleInterface fixSchedule, ScheduleInterface floatSchedule, ForwardCurveInterface forwardCurve) {
		return getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, null);
	}

	public static RandomVariableInterface getForwardSwapRate(ScheduleInterface fixSchedule, ScheduleInterface floatSchedule, ForwardCurveInterface forwardCurve, AnalyticModelInterface model) {
		DiscountCurveInterface discountCurve = model == null ? null : model.getDiscountCurve(forwardCurve.getDiscountCurveName());
		if(discountCurve == null) {
			discountCurve	= new DiscountCurveFromForwardCurve(forwardCurve.getName());
			model			= new AnalyticModel(new CurveInterface[] { forwardCurve, discountCurve });
		}

		double evaluationTime = fixSchedule.getFixing(0);	// Consider all values
		RandomVariableInterface swapAnnuity	= SwapAnnuity.getSwapAnnuity(evaluationTime, fixSchedule, discountCurve, model);

		// Create floating leg
		double fixing			= floatSchedule.getFixing(0);
		double payment			= floatSchedule.getPayment(0);
		double periodLength		= floatSchedule.getPeriodLength(0);

		RandomVariableInterface forward			= forwardCurve.getForward(model, fixing);
		RandomVariableInterface discountFactor	= discountCurve.getDiscountFactor(model, payment);

		RandomVariableInterface floatLeg = forward.mult(discountFactor).mult(periodLength);

		for(int periodIndex=1; periodIndex<floatSchedule.getNumberOfPeriods(); periodIndex++) {
			fixing			= floatSchedule.getFixing(periodIndex);
			payment			= floatSchedule.getPayment(periodIndex);
			periodLength		= floatSchedule.getPeriodLength(periodIndex);

			forward			= forwardCurve.getForward(model, fixing);
			discountFactor	= discountCurve.getDiscountFactor(model, payment);

			floatLeg = floatLeg.add(forward.mult(discountFactor).mult(periodLength));
		}

		RandomVariableInterface valueFloatLeg = floatLeg.div(discountCurve.getDiscountFactor(model, evaluationTime));

		return valueFloatLeg.div(swapAnnuity);
	}

	/**
	 * Return the receiver leg of the swap, i.e. the leg who's value is added to the swap value.
	 *
	 * @return The receiver leg of the swap.
	 */
	public AnalyticProductInterface getLegReceiver() {
		return legReceiver;
	}

	/**
	 * Return the payer leg of the swap, i.e. the leg who's value is subtracted from the swap value.
	 *
	 * @return The payer leg of the swap.
	 */
	public AnalyticProductInterface getLegPayer() {
		return legPayer;
	}

	@Override
	public String toString() {
		return "Swap [legReceiver=" + legReceiver + ", legPayer=" + legPayer
				+ "]";
	}
}
