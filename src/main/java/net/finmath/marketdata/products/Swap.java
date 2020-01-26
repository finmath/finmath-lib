/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 26.11.2012
 */
package net.finmath.marketdata.products;

import java.io.Serializable;

import net.finmath.marketdata.model.AnalyticModel;
import net.finmath.marketdata.model.AnalyticModelFromCurvesAndVols;
import net.finmath.marketdata.model.curves.Curve;
import net.finmath.marketdata.model.curves.DiscountCurve;
import net.finmath.marketdata.model.curves.DiscountCurveFromForwardCurve;
import net.finmath.marketdata.model.curves.ForwardCurve;
import net.finmath.modelling.DescribedProduct;
import net.finmath.modelling.InterestRateProductDescriptor;
import net.finmath.modelling.descriptor.InterestRateSwapProductDescriptor;
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
public class Swap extends AbstractAnalyticProduct implements AnalyticProduct, DescribedProduct<InterestRateSwapProductDescriptor>, Serializable {

	private static final long serialVersionUID = 6546984174616265190L;

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
			final String discountCurvePayName) {
		this(scheduleReceiveLeg, forwardCurveReceiveName, spreadReceive, discountCurveReceiveName, schedulePayLeg, forwardCurvePayName, spreadPay, discountCurvePayName, true);
	}

	/**
	 * Creates a swap with notional exchange. The swap has a unit notional of 1.
	 *
	 * @param scheduleReceiveLeg ScheduleFromPeriods of the receiver leg.
	 * @param spreadReceive Fixed spread on the forward or fix rate.
	 * @param discountCurveReceiveName Name of the discount curve for the receiver leg.
	 * @param schedulePayLeg ScheduleFromPeriods of the payer leg.
	 * @param forwardCurvePayName Name of the forward curve, leave empty if this is a fix leg.
	 * @param discountCurvePayName Name of the discount curve for the payer leg.
	 */
	public Swap(final Schedule scheduleReceiveLeg,
			final double spreadReceive,
			final String discountCurveReceiveName,
			final Schedule schedulePayLeg,
			final String forwardCurvePayName,
			final String discountCurvePayName) {
		this(scheduleReceiveLeg, null, spreadReceive, discountCurveReceiveName, schedulePayLeg, forwardCurvePayName, 0.0, discountCurvePayName, true);
	}


	@Override
	public double getValue(final double evaluationTime, final AnalyticModel model) {

		final double valueReceiverLeg	= legReceiver.getValue(evaluationTime, model);
		final double valuePayerLeg	= legPayer.getValue(evaluationTime, model);

		return valueReceiverLeg - valuePayerLeg;
	}

	public static double getForwardSwapRate(final TimeDiscretization fixTenor, final TimeDiscretization floatTenor, final ForwardCurve forwardCurve) {
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve);
	}

	public static double getForwardSwapRate(final TimeDiscretization fixTenor, final TimeDiscretization floatTenor, final ForwardCurve forwardCurve, final DiscountCurve discountCurve) {
		AnalyticModelFromCurvesAndVols model = null;
		if(discountCurve != null) {
			model			= new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve, discountCurve });
		}
		return getForwardSwapRate(new RegularSchedule(fixTenor), new RegularSchedule(floatTenor), forwardCurve, model);
	}

	public static double getForwardSwapRate(final Schedule fixSchedule, final Schedule floatSchedule, final ForwardCurve forwardCurve) {
		return getForwardSwapRate(fixSchedule, floatSchedule, forwardCurve, null);
	}

	public static double getForwardSwapRate(final Schedule fixSchedule, final Schedule floatSchedule, final ForwardCurve forwardCurve, AnalyticModel model) {
		DiscountCurve discountCurve = model == null ? null : model.getDiscountCurve(forwardCurve.getDiscountCurveName());
		if(discountCurve == null) {
			discountCurve	= new DiscountCurveFromForwardCurve(forwardCurve.getName());
			model			= new AnalyticModelFromCurvesAndVols(new Curve[] { forwardCurve, discountCurve });
		}

		final double evaluationTime = fixSchedule.getFixing(0);	// Consider all values
		final double swapAnnuity	= SwapAnnuity.getSwapAnnuity(evaluationTime, fixSchedule, discountCurve, model);

		double floatLeg = 0;
		for(int periodIndex=0; periodIndex<floatSchedule.getNumberOfPeriods(); periodIndex++) {
			final double fixing			= floatSchedule.getFixing(periodIndex);
			final double payment			= floatSchedule.getPayment(periodIndex);
			final double periodLength		= floatSchedule.getPeriodLength(periodIndex);

			final double forward			= forwardCurve.getForward(model, fixing, payment-fixing);
			final double discountFactor	= discountCurve.getDiscountFactor(model, payment);

			floatLeg += forward * periodLength * discountFactor;
		}

		final double valueFloatLeg = floatLeg / discountCurve.getDiscountFactor(model, evaluationTime);

		final double swapRate = valueFloatLeg / swapAnnuity;

		//		System.out.println(forwardCurve.getName() + "\t" + discountCurve.getName() + "\t" + swapRate);
		return swapRate;
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

	@Override
	public InterestRateSwapProductDescriptor getDescriptor() {
		// TODO what if it is not a swap leg?
		if(!(legReceiver instanceof DescribedProduct<?>) || !(legPayer instanceof DescribedProduct<?>)) {
			throw new RuntimeException("One or both of the legs of this swap do not support extraction of a descriptor.");
		}
		final InterestRateProductDescriptor receiverDescriptor = ((DescribedProduct<InterestRateProductDescriptor>) legReceiver).getDescriptor();
		final InterestRateProductDescriptor payerDescriptor = ((DescribedProduct<InterestRateProductDescriptor>) legPayer).getDescriptor();
		return new  InterestRateSwapProductDescriptor(receiverDescriptor, payerDescriptor);
	}
}
